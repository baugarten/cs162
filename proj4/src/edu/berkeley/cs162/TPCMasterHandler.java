/**
 * Handle TPC connections over a socket interface
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs162;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 *
 */
public class TPCMasterHandler implements NetworkHandler {
	private KVServer kvServer = null;
	private ThreadPool threadpool = null;
	private TPCLog tpcLog = null;
	
	private long slaveID = -1;
	
	// Used to handle the "ignoreNext" message
	private boolean ignoreNext = false;
	public boolean ignoreNextCommit = false;
	
	// States carried from the first to the second phase of a 2PC operation
	private KVMessage originalMessage = null;
	private boolean aborted = true;	

	public TPCMasterHandler(KVServer keyserver) {
		this(keyserver, 1);
	}

	public TPCMasterHandler(KVServer keyserver, long slaveID) {
		this.kvServer = keyserver;
		this.slaveID = slaveID;
		threadpool = new ThreadPool(1);
	}

	public TPCMasterHandler(KVServer kvServer, long slaveID, int connections) {
		this.kvServer = kvServer;
		this.slaveID = slaveID;
		threadpool = new ThreadPool(connections);
	}

	private class ClientHandler implements Runnable {
		private KVServer keyserver = null;
		private Socket client = null;
		
		private void closeConn() {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		
		@Override
		public void run() {
			// Receive message from client
			// Implement me
			KVMessage msg = null;
			try {
				msg = new KVMessage(client.getInputStream());
			} catch (KVException e) {
				KVMessage m = e.getMsg();
				System.err.println(m.getMsgType());
				System.err.println(m.getMessage());
				e.printStackTrace();
				return;
			} catch (Exception e1) {
				// burn in hell
				e1.printStackTrace();
				System.err.println("burnt in hell");
				return;
			} 
			
			//System.out.println(msg.toString());
			
			// Parse the message and do stuff 
			String key = msg.getKey();
			if (msg.getMsgType().equals("putreq")) {
				handlePut(msg, key);
			}
			else if (msg.getMsgType().equals("getreq")) {
				if (ignoreNextCommit) {
					ignoreNextCommit = false;
					try {
						KVMessage ret = new KVMessage("resp", "IgnoreNext Error: SlaveServer " + slaveID + " has ignored this 2PC request during the first phase");
						ret.setTpcOpId(msg.getTpcOpId());
						ret.sendMessage(client);
					} catch (KVException e) {
						// ignore
					}
					return;
				}
				
				handleGet(msg, key);
			}
			else if (msg.getMsgType().equals("delreq")) {
				handleDel(msg, key);
			} 
			else if (msg.getMsgType().equals("ignoreNext")) {
				ignoreNext = true;

				try {
					msg = new KVMessage("resp", "Success");
					msg.sendMessage(client);
				} catch (KVException e) {
					// ignore
				}
			}
			else if (msg.getMsgType().equals("commit") || msg.getMsgType().equals("abort")) {
				// Check in TPCLog for the case when SlaveServer is restarted
				// Implement me
				
				if (ignoreNext) {
					ignoreNext = false;
					return;
				}
				if (ignoreNextCommit) {
					ignoreNextCommit = false;
					try {
						KVMessage ret = new KVMessage("resp", "IgnoreNext Error: SlaveServer " + slaveID + " has ignored this 2PC request during the first phase");
						ret.setTpcOpId(msg.getTpcOpId());
						ret.sendMessage(client);
					} catch (KVException e) {
						// ignore
					}
					return;
				}
				
				if (tpcLog.hasInterruptedTpcOperation()) {
					originalMessage = tpcLog.getInterruptedTpcOperation();
				}
				handleMasterResponse(msg, originalMessage, aborted);
				originalMessage = null;
				
				// Reset state
				// Implement me
			} else {
				try {
					new KVMessage("resp", "Unknown Error: invalid message type").sendMessage(client);
				} catch (KVException e) {
					e.printStackTrace();
				}
			}
			
			// Finally, close the connection
			closeConn();
		}

		private void handlePut(KVMessage msg, String key) {
			AutoGrader.agTPCPutStarted(slaveID, msg, key);
			aborted = true;
			tpcLog.appendAndFlush(msg);
			if (ignoreNext) {
				ignoreNext = false;
				try {
					KVMessage ret = new KVMessage("abort", "IgnoreNext Error: SlaveServer " + slaveID + " has ignored this 2PC request during the first phase");
					ret.setTpcOpId(msg.getTpcOpId());
					ret.sendMessage(client);
				} catch (KVException e) {
					// ignore
				}
				AutoGrader.agTPCPutFinished(slaveID, msg, key);
				return;
			}
			KVMessage result = validateMessage(msg);
			if(result == null){
				AutoGrader.agTPCPutFinished(slaveID, msg, key);
				return;
			}
			originalMessage = new KVMessage(msg);
			
			try {
				aborted = false;
				KVMessage resp = new KVMessage("ready");
				resp.setTpcOpId(msg.getTpcOpId());
				resp.sendMessage(client);
				//System.out.println(resp.toString());
			} catch (KVException e) {
				// ignore
			}
			
			AutoGrader.agTPCPutFinished(slaveID, msg, key);
		}
		
		private KVMessage validateMessage(KVMessage msg) {
			if (oversizedKey(msg)) {
				try {
					KVMessage ret = new KVMessage("abort", "Oversized key");
					ret.setTpcOpId(msg.getTpcOpId());
					ret.sendMessage(client);
					return null;
				} catch (KVException e) {
					return null;
				}
			}
			if (oversizedValue(msg)) {
				try {
					KVMessage ret = new KVMessage("abort", "Oversized value");
					ret.setTpcOpId(msg.getTpcOpId());
					ret.sendMessage(client);
					return null;
				} catch (KVException e) {
					return null;
				}
			}
			return msg;
		}

		private boolean oversizedKey(KVMessage msg) {
			return msg.getKey().length() > 256;
		}
		private boolean oversizedValue(KVMessage msg) {
			return msg.getValue().length() > 256000;
		}

		private void handleGet(KVMessage msg, String key) {
 			AutoGrader.agGetStarted(slaveID);
 			if (oversizedKey(msg)) {
				try {
					KVMessage ret = new KVMessage("abort", "Oversized key");
					ret.setTpcOpId(msg.getTpcOpId());
					ret.sendMessage(client);
				} catch (KVException e) {
					// ignore
				}
	 			AutoGrader.agGetFinished(slaveID);
				return;
 			}
 			String value = null;
 			try {
 				value = keyserver.get(key);
 			} catch (KVException e) {
 				KVMessage message = e.getMsg();
 				message.setTpcOpId(message.getTpcOpId());
 				try {
					message.sendMessage(client);
				} catch (KVException e1) {
					e1.printStackTrace();
				}
 			}
 			try {
	 			if (value != null) {
		 			KVMessage ret = new KVMessage("resp");
		 			ret.setKey(key);
		 			ret.setValue(value);
					ret.setTpcOpId(msg.getTpcOpId());
		 			ret.sendMessage(client);
	 			} else {
	 				KVMessage resp = new KVMessage("resp", "Does not exist");
					resp.setTpcOpId(msg.getTpcOpId());
					resp.sendMessage(client);
	 			}
 			} catch (KVException e) {
 				//ignore
 			}
 			
 			AutoGrader.agGetFinished(slaveID);
		}
		
		private void handleDel(KVMessage msg, String key) {
			AutoGrader.agTPCDelStarted(slaveID, msg, key);
			tpcLog.appendAndFlush(msg);
			
			if (ignoreNext) {
				ignoreNext = false;
				try {
					aborted = true;
					KVMessage resp = new KVMessage("abort", "IgnoreNext Error: SlaveServer " + slaveID + " has ignored this 2PC request during the first phase");
					resp.setTpcOpId(msg.getTpcOpId());
					resp.sendMessage(client);
				} catch (KVException e) {
					// ignore
				}
				AutoGrader.agTPCDelFinished(slaveID, msg, key);
				return;
			}
 			if (oversizedKey(msg)) {
				try {
					aborted = true;
					KVMessage resp = new KVMessage("abort", "Oversized value");
					resp.setTpcOpId(msg.getTpcOpId());
					resp.sendMessage(client);
				} catch (KVException e) {
					// ignore
				}
	 			AutoGrader.agGetFinished(slaveID);
				return;
 			}
 			String val = null;
 			try {
				val = kvServer.get(key);
			} catch (KVException e) {
				e.printStackTrace();
			} finally {
				if (val == null) { // We don't have a valid key
					try {
						aborted = true;
						KVMessage resp = new KVMessage("abort", "Does not exist");
						resp.setTpcOpId(msg.getTpcOpId());
						resp.sendMessage(client);
					} catch (KVException e) {
						// ignore
					}
					AutoGrader.agTPCDelFinished(slaveID, msg, key);
					return;
				}
			}

			// Store for use in the second phase
			originalMessage = new KVMessage(msg);
			
			try {
				aborted = false;
				KVMessage resp = new KVMessage("ready");
				resp.setTpcOpId(msg.getTpcOpId());
				resp.sendMessage(client);
			} catch (KVException e) {
				// ignore
			}
			
			AutoGrader.agTPCDelFinished(slaveID, msg, key);
		}

		/**
		 * Second phase of 2PC
		 * 
		 * @param masterResp Global decision taken by the master
		 * @param origMsg Message from the actual client (received via the coordinator/master)
		 * @param origAborted Did this slave server abort it in the first phase 
		 */
		private void handleMasterResponse(KVMessage masterResp, KVMessage origMsg, boolean origAborted) {
			AutoGrader.agSecondPhaseStarted(slaveID, origMsg, origAborted);
			
			tpcLog.appendAndFlush(masterResp);
			
			if (origAborted || masterResp.getMsgType().equals("abort")) {
				originalMessage = null;
				try {
					KVMessage resp = new KVMessage("ack");
					resp.setTpcOpId(masterResp.getTpcOpId());
					resp.sendMessage(client);
					//System.out.println(resp.toString());
				} catch (KVException e) {
					// ignore
				}
				AutoGrader.agSecondPhaseFinished(slaveID, origMsg, origAborted);
				return;
			}
			if (masterResp.getMsgType().equals("commit")) {
				if (originalMessage.getMsgType().equals("putreq")) {
					try {
						keyserver.put(origMsg.getKey(), origMsg.getValue());
						KVMessage resp = new KVMessage("ack");
						resp.setTpcOpId(masterResp.getTpcOpId());
						resp.sendMessage(client);
						//System.out.println(resp.toString());
					} catch (KVException e) {
						// return failure
					}
				} else {
					try {
						keyserver.del(origMsg.getKey());
						KVMessage resp = new KVMessage("ack");
						resp.setTpcOpId(masterResp.getTpcOpId());
						resp.sendMessage(client);
						//System.out.println(resp.toString());
					} catch (KVException e) {
						// ignore
					}
				}
			}
			
			// Implement me
			
			AutoGrader.agSecondPhaseFinished(slaveID, origMsg, origAborted);
		}

		public ClientHandler(KVServer keyserver, Socket client) {
			this.keyserver = keyserver;
			this.client = client;
		}
	}

	@Override
	public void handle(Socket client) throws IOException {
		AutoGrader.agReceivedTPCRequest(slaveID);
		Runnable r = new ClientHandler(kvServer, client);
		try {
			threadpool.addToQueue(r);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}		
		AutoGrader.agFinishedTPCRequest(slaveID);
	}

	/**
	 * Set TPCLog after it has been rebuilt
	 * @param tpcLog
	 */
	public void setTPCLog(TPCLog tpcLog) {
		this.tpcLog  = tpcLog;
	}

	/**
	 * Registers the slave server with the coordinator
	 * 
	 * @param masterHostName
	 * @param servr KVServer used by this slave server (contains the hostName and a random port)
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws KVException
	 */
	public void registerWithMaster(String masterHostName, SocketServer server) throws UnknownHostException, IOException, KVException {
		AutoGrader.agRegistrationStarted(slaveID);
		
		Socket master = new Socket(masterHostName, 9090);
		KVMessage regMessage = new KVMessage("register", slaveID + "@" + server.getHostname() + ":" + server.getPort());
		regMessage.sendMessage(master);
		
		System.out.println("Sent message to master");
		// Receive master response. 
		// Response should always be success, except for Exceptions. Throw away.
		KVMessage reg = new KVMessage(master.getInputStream());
		System.out.println("Received message: " + reg.getMessage());
		
		master.close();
		AutoGrader.agRegistrationFinished(slaveID);
	}
}

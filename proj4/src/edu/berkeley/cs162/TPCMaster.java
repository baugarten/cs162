/**
 * Master for Two-Phase Commits
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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class TPCMaster {
    TreeMap<Long, SlaveInfo> slaves;
    
	/**
	 * Implements NetworkHandler to handle registration requests from 
	 * SlaveServers.
	 * 
	 */
	private class TPCRegistrationHandler implements NetworkHandler {

		private ThreadPool threadpool = null;

		public TPCRegistrationHandler() {
			// Call the other constructor
			this(1);	
		}

		public TPCRegistrationHandler(int connections) {
			threadpool = new ThreadPool(connections);	
		}

		@Override
		public void handle(Socket client) throws IOException {
			try {
				Runnable r = new RegistrationHandler(client);
				threadpool.addToQueue(r);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

		private class RegistrationHandler implements Runnable {

			private Socket client = null;

			public RegistrationHandler(Socket client) {
				this.client = client;
			}

			@Override
			public void run() {
				// implement me
				try {
					InputStream input = client.getInputStream();
					String info= new Scanner(input).next();
					SlaveInfo slave = new SlaveInfo(info);
					slaves.put(slave.slaveID, slave);
				}
				catch (KVException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
	}
	
	/**
	 *  Data structure to maintain information about SlaveServers
	 *
	 */
	private class SlaveInfo {
		// 64-bit globally unique ID of the SlaveServer
		private long slaveID = -1;
		// Name of the host this SlaveServer is running on
		private String hostName = null;
		// Port which SlaveServer is listening to
		private int port = -1;

		/**
		 * 
		 * @param slaveInfo as "SlaveServerID@HostName:Port"
		 * @throws KVException
		 */
		public SlaveInfo(String slaveInfo) throws KVException {
			int aPos = slaveInfo.indexOf('@');
			int cPos = slaveInfo.indexOf(':');
			if (aPos > cPos || aPos < 0 || cPos < 0) {
				throw new KVException(new KVMessage("Registration Error: Received unparseable slave information"));
			}
			
			String idStr = slaveInfo.substring(0, aPos);
			hostName = slaveInfo.substring(aPos+1, cPos);
			String portStr = slaveInfo.substring(cPos+1, slaveInfo.length());
			
			try {
				slaveID = hashTo64bit(idStr);
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException e) {
				throw new KVException(new KVMessage("Registration Error: Received unparseable slave information"));
			}
			if (port > 65535) {
				throw new KVException(new KVMessage("Registration Error: Received unparseable slave information"));
			}
		}
		
		public long getSlaveID() {
			return slaveID;
		}
		
		public KVMessage doKVOperation(KVMessage op) throws KVException {
			try {
				Socket socket = new Socket(this.hostName, this.port);
				socket.setSoTimeout(TIMEOUT_MILLISECONDS);
				
				op.sendMessage(socket);
				KVMessage response = new KVMessage(socket.getInputStream());
				socket.close();
				
				return response;
			} catch (SocketTimeoutException e) {
				return null;
			} catch (IOException e) {
				throw new KVException(new KVMessage("resp", "Network Error: Could not create socket"));
			}
		}
		
		String slaveGet(String key) {
			try {
				KVMessage msg = new KVMessage("getreq");
				msg.setKey(key);
				KVMessage resp = doKVOperation(msg);
				if (resp != null && resp.getMsgType() == "resp" && resp.getKey() == key) {
					return resp.getValue();
				} else {
					return null;
				}
			} catch (KVException e) {
				return null;
			}
		}
		
		boolean tpcPutVote(String key, String val, String tpcOpId) {
			try {
				KVMessage msg = new KVMessage("putreq");
				msg.setKey(key);
				msg.setValue(val);
				msg.setTpcOpId(tpcOpId);
				KVMessage resp = doKVOperation(msg);
				if (resp != null && resp.getMsgType() == "ready") {
					return true;
				} else {
					return false;
				}
			} catch (KVException e) {
				return false;
			}
		}
		
		boolean tpcDeleteVote(String key, String tpcOpId) {
			try {
				KVMessage msg = new KVMessage("delreq");
				msg.setKey(key);
				msg.setTpcOpId(tpcOpId);
				KVMessage resp = doKVOperation(msg);
				if (resp != null && resp.getMsgType() == "ready") {
					return true;
				} else {
					return false;
				}
			} catch (KVException e) {
				return false;
			}
		}
		
		void tpcCommit(String tpcOpId) {
			boolean done = false;
			while (!done) {
				try {
					KVMessage msg = new KVMessage("commit");
					msg.setTpcOpId(tpcOpId);
					KVMessage resp = doKVOperation(msg);
					if (resp != null && resp.getMsgType() == "ack" && resp.getTpcOpId() == tpcOpId) {
						done = true;
					}
				} catch (KVException e) {
					
				}
			}
		}
		
		void tpcAbort(String tpcOpId) {
			boolean done = false;
			while (!done) {
				try {
					KVMessage msg = new KVMessage("abort");
					msg.setTpcOpId(tpcOpId);
					KVMessage resp = doKVOperation(msg);
					if (resp != null && resp.getMsgType() == "ack" && resp.getTpcOpId() == tpcOpId) {
						done = true;
					}
				} catch (KVException e) {
					
				}
			}
		}
	}
	
	// Timeout value used during 2PC operations
	private static final int TIMEOUT_MILLISECONDS = 5000;
	
	// Cache stored in the Master/Coordinator Server
	private KVCache masterCache = new KVCache(100, 10);
	
	// Registration server that uses TPCRegistrationHandler
	private SocketServer regServer = null;

	// Number of slave servers in the system
	private int numSlaves = -1;
	
	// ID of the next 2PC operation
	private Long tpcOpId = 0L;
	
	private class IDComparator implements Comparator<Long>{

		@Override
		public int compare(Long arg0, Long arg1) {
			if(isLessThanUnsigned(arg0.longValue(),arg1.longValue())){
				return -1;
			} else if (arg0.compareTo(arg1) == 0){
				return 0;
			} else {
				return 1;
			}
		}
		
	}
	
	/**
	 * Creates TPCMaster
	 * 
	 * @param numSlaves number of expected slave servers to register
	 * @throws Exception
	 */
	public TPCMaster(int numSlaves) {
		// Using SlaveInfos from command line just to get the expected number of SlaveServers 
		this.numSlaves = numSlaves;
		slaves = new TreeMap<Long,SlaveInfo>(new IDComparator());

		// Create registration server
		regServer = new SocketServer("localhost", 9090);
	}
	
	/**
	 * Calculates tpcOpId to be used for an operation. In this implementation
	 * it is a long variable that increases by one for each 2PC operation. 
	 * 
	 * @return 
	 */
	private String getNextTpcOpId() {
		tpcOpId++;
		return tpcOpId.toString();		
	}
	
	/**
	 * Start registration server in a separate thread
	 */
	public void run() {
		AutoGrader.agTPCMasterStarted();
		//implement me
		regServer.addHandler(new TPCRegistrationHandler());
		Thread t =new Thread(new Runnable() {
			public void run() {
				try {
					regServer.connect();
					regServer.run();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();

		AutoGrader.agTPCMasterFinished();
	}
	
	/**
	 * Converts Strings to 64-bit longs
	 * Borrowed from http://stackoverflow.com/questions/1660501/what-is-a-good-64bit-hash-function-in-java-for-textual-strings
	 * Adapted from String.hashCode()
	 * @param string String to hash to 64-bit
	 * @return
	 */
	private long hashTo64bit(String string) {
		// Take a large prime
		long h = 1125899906842597L; 
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31*h + string.charAt(i);
		}
		return h;
	}
	
	/**
	 * Compares two longs as if they were unsigned (Java doesn't have unsigned data types except for char)
	 * Borrowed from http://www.javamex.com/java_equivalents/unsigned_arithmetic.shtml
	 * @param n1 First long
	 * @param n2 Second long
	 * @return is unsigned n1 less than unsigned n2
	 */
	private boolean isLessThanUnsigned(long n1, long n2) {
		return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
	}
	
	private boolean isLessThanEqualUnsigned(long n1, long n2) {
		return isLessThanUnsigned(n1, n2) || n1 == n2;
	}	

	/**
	 * Find first/primary replica location
	 * @param key
	 * @return
	 */
	private SlaveInfo findFirstReplica(String key) {
		// 64-bit hash of the key
		long hashedKey = hashTo64bit(key.toString());
		Long keyInMap = new Long(hashedKey);
		if(slaves.containsKey(keyInMap)){
			return slaves.get(keyInMap);
		} else {
			Long replica = slaves.higherKey(keyInMap);
			if (replica == null) {
				return slaves.firstEntry().getValue();
			} else {
				return slaves.get(replica);
			}
		}
	}
	
	/**
	 * Find the successor of firstReplica to put the second replica
	 * @param firstReplica
	 * @return
	 */
	private SlaveInfo findSuccessor(SlaveInfo firstReplica) {
		Long firstReplicaID = new Long(firstReplica.slaveID);
		Long successor = slaves.higherKey(firstReplicaID);
		if (successor == null){
			return slaves.firstEntry().getValue();
		} else {
			return slaves.get(successor);
		}
	}
	
	/**
	 * Synchronized method to perform 2PC operations one after another
	 * You will need to remove the synchronized declaration if you wish to attempt the extra credit
	 * 
	 * @param msg
	 * @param isPutReq
	 * @throws KVException
	 */
	public void performTPCOperation(KVMessage msg, boolean isPutReq) throws KVException {
		AutoGrader.agPerformTPCOperationStarted(isPutReq);
		
		String key = msg.getKey();
		
		WriteLock masterLock = masterCache.getWriteLock(key);
		masterLock.lock();
		
		SlaveInfo slave1 = findFirstReplica(key);
		SlaveInfo slave2 = findSuccessor(slave1);
		String tpcOpId = getNextTpcOpId();
		
		SlaveVoteRunnable slaveVote1 = new SlaveVoteRunnable(slave1, key, msg.getValue(), tpcOpId, isPutReq);
		SlaveVoteRunnable slaveVote2 = new SlaveVoteRunnable(slave2, key, msg.getValue(), tpcOpId, isPutReq);
		Thread slaveThread1 = new Thread(slaveVote1);
		Thread slaveThread2 = new Thread(slaveVote2);
		slaveThread1.start();
		slaveThread2.start();
		
		try {
			slaveThread1.join();
			slaveThread2.join();
		} catch (InterruptedException e) {
			masterLock.unlock();
			AutoGrader.agPerformTPCOperationFinished(isPutReq);
			throw new KVException(new KVMessage("Internal: performTPCOperation failed: InterruptedException"));
		}
		
		boolean doCommit = slaveVote1.returnVal && slaveVote2.returnVal;
		SlaveDecisionRunnable slaveDecision1 = new SlaveDecisionRunnable(slave1, tpcOpId, doCommit);
		SlaveDecisionRunnable slaveDecision2 = new SlaveDecisionRunnable(slave2, tpcOpId, doCommit);
		slaveThread1 = new Thread(slaveDecision1);
		slaveThread2 = new Thread(slaveDecision2);
		slaveThread1.start();
		slaveThread2.start();
		
		if (doCommit) {
			// do cache commit in main thread
			if (isPutReq) {
				masterCache.put(key, msg.getValue());
			} else {	// cache delete
				masterCache.del(key);
			}
		}
		
		try{
			slaveThread1.join();
			slaveThread2.join();
		} catch (InterruptedException e) {	
			masterLock.unlock();
			AutoGrader.agPerformTPCOperationFinished(isPutReq);
			throw new KVException(new KVMessage("Internal: performTPCOperation failed: InterruptedException"));
		}
		
		if (!doCommit) {
			masterLock.unlock();
			AutoGrader.agPerformTPCOperationFinished(isPutReq);
			throw new KVException(new KVMessage("Internal: performTPCOperation failed: TPC refused"));
		}
		
		masterLock.unlock();
		AutoGrader.agPerformTPCOperationFinished(isPutReq);
		return;
	}

	public class SlaveDecisionRunnable implements Runnable {
		SlaveInfo slave;
		String tpcOpId;
		boolean isCommit;

		public SlaveDecisionRunnable(SlaveInfo slave, String tpcOpId, boolean isCommit) {
			this.slave = slave;
			this.tpcOpId = tpcOpId;
			this.isCommit = isCommit;
		}
		
		@Override
		public void run() {
			if (isCommit) {
				slave.tpcCommit(tpcOpId);
			} else {	// abort
				slave.tpcAbort(tpcOpId);
			}
		}
	}
	
	public class SlaveVoteRunnable implements Runnable {
		boolean returnVal = false;
		
		SlaveInfo slave;
		String key, val, tpcOpId;
		boolean isPutReq;
		
		public SlaveVoteRunnable(SlaveInfo slave, String key, String val, String tpcOpId, boolean isPutReq) {
			this.slave = slave;
			this.key = key;
			this.val = val;
			this.tpcOpId = tpcOpId;
			this.isPutReq = isPutReq;
		}
		
		@Override
		public void run() {
			if (isPutReq) {
				returnVal = slave.tpcPutVote(key, val, tpcOpId);
			} else {	// delete req
				returnVal = slave.tpcDeleteVote(key, tpcOpId);
			}
		}
	}
	
	/**
	 * Perform GET operation in the following manner:
	 * - Try to GET from first/primary replica
	 * - If primary succeeded, return Value
	 * - If primary failed, try to GET from the other replica
	 * - If secondary succeeded, return Value
	 * - If secondary failed, return KVExceptions from both replicas
	 * 
	 * @param msg Message containing Key to get
	 * @return Value corresponding to the Key
	 * @throws KVException
	 */
	public String handleGet(KVMessage msg) throws KVException {
		AutoGrader.aghandleGetStarted();

		String key = msg.getKey();
		
		WriteLock masterLock = masterCache.getWriteLock(key);
		masterLock.lock();
		String cacheVal = masterCache.get(key);
		if (cacheVal != null) {
			masterLock.unlock();
			AutoGrader.aghandleGetFinished();
			return cacheVal;
		}
		
		Semaphore masterWake = new Semaphore(0);
		SlaveInfo slave1 = findFirstReplica(key);
		SlaveInfo slave2 = findSuccessor(slave1);
		SlaveGetRunnable slaveGet1 = new SlaveGetRunnable(slave1, key, masterWake);
		SlaveGetRunnable slaveGet2 = new SlaveGetRunnable(slave2, key, masterWake);
		Thread slaveThread1 = new Thread(slaveGet1);
		Thread slaveThread2 = new Thread(slaveGet2);
		slaveThread1.start();
		slaveThread2.start();
		
		try {
			masterWake.acquire();
		} catch (InterruptedException e) {
			masterLock.unlock();
			AutoGrader.aghandleGetFinished();
			throw new KVException(new KVMessage("Internal: performTPCOperation failed: InterruptedException"));
		}
		
		String returned;
		if (slaveGet1.done) {
			returned = slaveGet1.returnVal;
		} else if (slaveGet2.done) {
			returned = slaveGet1.returnVal;
		} else {
			masterLock.unlock();
			AutoGrader.aghandleGetFinished();
			throw new KVException(new KVMessage("Internal: performTPCOperation failed: This really shouldn't happen"));
		}
		
		// update cache
		if (returned != null) {
			masterCache.put(key, returned);
		}
		
		masterLock.unlock();
		
		AutoGrader.aghandleGetFinished();
		return returned;
	}
	
	public class SlaveGetRunnable implements Runnable {
		String returnVal = null;
		boolean done = false;
		Semaphore masterWake;
		
		SlaveInfo slave;
		String key;
		
		public SlaveGetRunnable(SlaveInfo slave, String key, Semaphore masterWake) {
			this.slave = slave;
			this.key = key;
			this.masterWake = masterWake;
		}
		
		@Override
		public void run() {
			returnVal = slave.slaveGet(key);
			done = true;
			masterWake.release();
		}
		
		
	}
}

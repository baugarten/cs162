/**
 * Autograder for the Key-Value Store.
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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AutoGrader {
	
	/**
	 * Information for individual operations
	 */
	private static class TestOperation {
		// Thread to execute this operation in
		Thread execThread = null;
		
		// Time to wait before calling the operation
		long delayBeforeOp = 0;
		
		// Name of the operation
		String opName = null;
		
		// Key and value
		String key = null;
		String value = null;
		
		// Time to wait in KVCache during the opertion
		long putDelay = 0;
		long getDelay = 0;
		long delDelay = 0;
		
		TestOperation(long delayBeforeOp, String opName, String key, String value, long delay) {
			this.delayBeforeOp = delayBeforeOp;

			this.opName = opName.toUpperCase();
			this.key = key;
			this.value = value;
			
			if (opName.equals("PUT")) {
				this.putDelay = delay;
			} else if (opName.equals("GET")) {
				this.getDelay = delay;
			} else if (opName.equals("DEL")) {
				this.delDelay = delay;
			}
		}
		
		void executeInThread() {
			Runnable r = new Runnable() {
				public void run() {
					// Create a client
					KVClient kc = new KVClient("localhost", 8080);
					System.out.println(opName);

					// Perform the operation
					try {
						if (opName.equals("PUT")) {
							kc.put(key, value);
						} else if (opName.equals("GET")) {
							kc.get(key);
						} else if (opName.equals("DEL")) {
							kc.del(key);
						}
					} catch (KVException kve) {
						System.err.println(kve.getMsg());
					}
				}
			};

			execThread = new Thread(r);
			execThread.start();
		}
		
		void waitToFinish() {
			try {
				execThread.join();
			} catch (Exception e) {
			}
		}
	}
	
	private static long STORE_DELAY = 1000;
	
	private static KVStore dataStore = null;
	private static KVCache dataCache = null;
	
	private static int currentOp = 0;
	private static TestOperation[] TEST_OPS = null;
	
	private static TPCMasterServerThread masterThread = null;
	private static ServerThread[] serverThreads = null;
	private static int curTestID = -1; // Currently running test
	
	private static ArrayList<Long> killAtFirstPhase = new ArrayList<Long>();
	
	private static HashMap<Long, Thread> slaveID2ThreadMap = new HashMap<Long, Thread>();
	
	public static void main(String args[]) {
		runFirstPhaseFailureTest();
	}
	
	private static void runTest(int numSets, int maxElemsPerSet, long storeDelay, int numSlaves, TestOperation[] testOps) 
	{
		// Initialize
		currentOp = 0;
		TEST_OPS = testOps;
		
		// Set STORE_DELAY
		STORE_DELAY = storeDelay;
		
		long[] slaveIds = new long[numSlaves];
		Random slaveIdGenerator = new Random();
		
		for (int i=0; i < numSlaves; i++) {
			slaveIds[i] = slaveIdGenerator.nextLong();
		}
		
		// Start TPC Master		
		TPCMasterServerThread masterThread = startMasterServer(numSets, maxElemsPerSet, numSlaves);
		ServerThread[] serverThreads = new ServerThread[numSlaves];

		for (int i=0; i < slaveIds.length; i++) {
			// Start kvServer
			serverThreads[i] = startSlaveServer(numSets, maxElemsPerSet, slaveIds[i], "localhost");
			
			// Store mapping from slaveIDs to corresponding threads
			slaveID2ThreadMap.put(slaveIds[i], serverThreads[i]);
			
			delay(500); 
		}
		
		// Iterate over TestOperation[]
		for (TestOperation testOp: testOps) {
			
			// Delay the operation
			delay(testOp.delayBeforeOp);
			
			// Each TestOperation runs in its own thread
			testOp.executeInThread();
			
			// Move to the next TestOperation
			currentOp++;
		}
		
		// Wait for threads to finish
		for (TestOperation testOp: testOps) {
			testOp.waitToFinish();
		}

		
		for (int i=0; i < slaveIds.length; i++) {
			// Stop SocketServer
			serverThreads[i].stopServer();
		}
		
		masterThread.stopServer();
		
		// Cleanup
		currentOp = 0;
		TEST_OPS = null;
		
		// Give time to cleanup
		delay(5000);
	}
	
	
	private static void runFirstPhaseFailureTest() 
	{
		int numSets = 2;
		int maxElemsPerSet = 2;
		
		String three = "3";
		String seven = "7";
		
		ArrayList<TestOperation> testOps = new ArrayList<TestOperation>();
		
		// PUT (3, 7)
		testOps.add(new TestOperation(0, "PUT", three, seven, 0));
		
		// Wait 5 seconds; GET 3; Make it last for 10 seconds 
		testOps.add(new TestOperation(5000, "GET", three, null, 10000));
		
		long storeDelay =  STORE_DELAY;
		
		// Initialize
		currentOp = 0;
		TEST_OPS = (TestOperation[]) testOps.toArray();
		
		curTestID = 1;
		
		// Set STORE_DELAY
		STORE_DELAY = storeDelay;
		
		int numSlaves = 2;
		long[] slaveIds = new long[numSlaves];
		Random slaveIdGenerator = new Random();
		
		for (int i=0; i < numSlaves; i++) {
			slaveIds[i] = slaveIdGenerator.nextInt();
		}

		
		// Start TPC Master		
		masterThread = startMasterServer(numSets, maxElemsPerSet, numSlaves);
		serverThreads = new ServerThread[numSlaves];
		
		killAtFirstPhase = new ArrayList<Long>();
		for (int i=0; i < slaveIds.length; i++) {
			// Start kvServer
			serverThreads[i] = startSlaveServer(numSets, maxElemsPerSet, slaveIds[i], "localhost");
			if (killAtFirstPhase.size() == 0) {
				killAtFirstPhase.add(new Long(slaveIds[i]));
			}
			
			// Store mapping from slaveIDs to corresponding threads
			slaveID2ThreadMap.put(slaveIds[i], serverThreads[i]);
			
			delay(500); 
		}
		
		// Iterate over TestOperation[]
		for (TestOperation testOp: testOps) {
			
			// Delay the operation
			delay(testOp.delayBeforeOp);
			
			// Each TestOperation runs in its own thread
			testOp.executeInThread();
			
			// Move to the next TestOperation
			currentOp++;
		}
		
		// Wait for threads to finish
		for (TestOperation testOp: testOps) {
			testOp.waitToFinish();
		}

		
		for (int i=0; i < slaveIds.length; i++) {
			// Stop SocketServer
			serverThreads[i].stopServer();
		}
		
		masterThread.stopServer();
		
		// Cleanup
		currentOp = 0;
		TEST_OPS = null;
		
		killAtFirstPhase = null;
		masterThread = null;
		serverThreads = null;
		
		// Give time to cleanup
		delay(5000);
	}
	
	/**
	 * Keep track of KVServer life cycle for individual tests
	 */
	private static class ServerThread extends Thread {
		int numSets; 
		int maxElemsPerSet;
		SocketServer server = null;
		long slaveId = 0;
		String masterHostName;
        
        public ServerThread(int numSets, int maxElemsPerSet, long slaveId, String masterHostName) {
            this.numSets = numSets;
            this.maxElemsPerSet = maxElemsPerSet;
            this.slaveId = slaveId;
            this.masterHostName = masterHostName;
        }

		public void stopServer() {
			server.stop();
		}

		public void run() {
			KVServer kvServer = new KVServer(numSets, maxElemsPerSet);
			
			TPCMasterHandler handler = new TPCMasterHandler(kvServer, slaveId, 1);
			
			// Create TPCLog
            String logPath = slaveId + "@" + server.getHostname();
            TPCLog tpcLog = new TPCLog(logPath, kvServer);
            
            // Load from disk and rebuild logs
            try {
				tpcLog.rebuildKeyServer();
			} catch (KVException e2) {
				// TODO: Handle this
			}
            
            // Set log for TPCMasterHandler
            handler.setTPCLog(tpcLog);
            
			server = new SocketServer("localhost");
			server.addHandler(handler);
			try {
				server.connect();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			
			try {
				handler.registerWithMaster(masterHostName, server);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (KVException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}		

			System.out.println("Starting ServerThread at port " + server.getPort() + "...");
			try {
				server.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Stopping ServerThread...");
		}
    }
	
	/**
	 * Keep track of TPCMaster life cycle for individual tests
	 */
	private static class TPCMasterServerThread extends Thread {
		int numSets; 
		int maxElemsPerSet;
		SocketServer server = null;
		int numSlaves = 0;
        
		TPCMasterServerThread(int numSets, int maxElemsPerSet, int numSlaves) {
            this.numSets = numSets;
            this.maxElemsPerSet = maxElemsPerSet;
            this.numSlaves = numSlaves;
        }

		public void stopServer() {
			server.stop();
		}

		public void run() {
			TPCMaster tpcMaster = new TPCMaster(numSlaves);
			NetworkHandler handler = new KVClientHandler(tpcMaster);

			server = new SocketServer("localhost", 8080);
			server.addHandler(handler);
			try {
				server.connect();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}	
			System.out.println("Starting Registration Thread...");
			tpcMaster.run();
			
			System.out.println("Starting ServerThread...");
			try {
				server.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }

	private static TPCMasterServerThread startMasterServer(int numSets, int maxElemsPerSet, int numSlaves) {
		TPCMasterServerThread thr1 = new TPCMasterServerThread(numSets, maxElemsPerSet, numSlaves);		
		thr1.start();
		return thr1;
	}
	
	private static ServerThread startSlaveServer(int numSets, int maxElemsPerSet, long slaveId, String masterHostName) {
		ServerThread thr1 = new ServerThread(numSets, maxElemsPerSet, slaveId, masterHostName);		
		thr1.start();
		return thr1;
	}
		
	private static void testCase1() {
	}
	
	public static void registerKVServer(KVStore dataStore, KVCache dataCache) {
		AutoGrader.dataStore = dataStore;
		AutoGrader.dataCache = dataCache;
	}

	public static void agCachePutStarted(String key, String value) {
		
	}
	
	public static void agCachePutFinished(String key, String value) {
		
	}

	public static void agCacheGetStarted(String key) {
		
	}
	
	public static void agCacheGetFinished(String key) {
		
	}

	public static void agCacheDelStarted(String key) {
		
	}
	
	public static void agCacheDelFinished(String key) {
		
	}

	public static void agStorePutStarted(String key, String value) {
		
	}
	
	public static void agStorePutFinished(String key, String value) {
		
	}

	public static void agStoreGetStarted(String key) {
		
	}
	
	public static void agStoreGetFinished(String key) {
		
	}

	public static void agStoreDelStarted(String key) {
		
	}

	public static void agStoreDelFinished(String key) {
		
	}

	public static void agKVServerPutStarted(String key, String value) {
		
	}
	
	public static void agKVServerPutFinished(String key, String value) {
		
	}

	public static void agKVServerGetStarted(String key) {
		
	}
	
	public static void agKVServerGetFinished(String key) {
		
	}
	
	public static void agKVServerDelStarted(String key) {
		
	}

	public static void agKVServerDelFinished(String key) {
		
	}

	public static void agCachePutDelay() {

	}

	public static void agCacheGetDelay() {

	}
	
	public static void agCacheDelDelay() {

	}

	/**
	 * KVStore will sleep for STORE_DELAY milliseconds  
	 */
	public static void agStoreDelay() {
		delay(STORE_DELAY);
	}
	
	/**
	 * Helper method to put the current thread to sleep for sleepTime duration
	 * @param sleepTime time to sleep in milliseconds
	 */
	private static void delay(long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void agRegistrationStarted(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	public static void agRegistrationFinished(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	public static void agReceivedTPCRequest(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	public static void agFinishedTPCRequest(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	public static void agTPCMasterStarted() {
		// TODO Auto-generated method stub
		
	}

	public static void agTPCMasterFinished() {
		// TODO Auto-generated method stub
		
	}

	public static void aghandleGetFinished() {
		// TODO Auto-generated method stub
		
	}

	public static void aghandleGetStarted() {
		// TODO Auto-generated method stub
		
	}

	public static void agPerformTPCOperationFinished(boolean isPutReq) {
		// TODO Auto-generated method stub
		
	}

	public static void agPerformTPCOperationStarted(boolean isPutReq) {
		// TODO Auto-generated method stub
		
	}

	public static void agSecondPhaseStarted(long slaveID, KVMessage origMsg, boolean origAborted) {
		if (curTestID == 1 && !killAtFirstPhase.contains(new Long(slaveID))) {
			Thread srvThread = slaveID2ThreadMap.get(new Long(slaveID));
			for (int i=0; i < serverThreads.length; i++) {
				if (serverThreads[i] == srvThread) {
					serverThreads[i].run();
				}
			}
		}		
	}

	public static void agSecondPhaseFinished(long slaveID, KVMessage origMsg, boolean origAborted) {
		// TODO Auto-generated method stub
		
	}

	public static void agGetStarted(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	public static void agGetFinished(long slaveID) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	public static void agTPCPutStarted(long slaveID, KVMessage msg, String key) {
		if (curTestID == 1 && killAtFirstPhase.contains(new Long(slaveID))) {
			Thread srvThread = slaveID2ThreadMap.get(new Long(slaveID));
			srvThread.stop();
		}	
	}

	public static void agTPCPutFinished(long slaveID, KVMessage msg, String key) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	public static void agTPCDelStarted(long slaveID, KVMessage msg, String key) {
		if (curTestID == 1 && killAtFirstPhase.contains(new Long(slaveID))) {
			Thread srvThread = slaveID2ThreadMap.get(new Long(slaveID));
			srvThread.stop();
		}		
	}

	public static void agTPCDelFinished(long slaveID, KVMessage msg, String key) {
		// TODO Auto-generated method stub
		
	}

}

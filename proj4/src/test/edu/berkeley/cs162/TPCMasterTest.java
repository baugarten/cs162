package test.edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.*;

import java.io.*;
import java.util.*;

public class TPCMasterTest {
	static TPCMaster tpcMaster;
	
	static ArrayList<KVServer> slaveServers;
	
	static ArrayList<Thread> threads;
	
	@Before
	public void setUp() throws Exception {
		slaveServers = new ArrayList<KVServer>();
		threads = new ArrayList<Thread>();
		
		String masterHostName = InetAddress.getLocalHost().getHostAddress();
		int masterPort = 8080;
		
		// Create TPCMaster
		tpcMaster = new TPCMaster(3);
		tpcMaster.run();
		
		// Create KVClientHandler
		System.out.println("Binding Master:");
		final SocketServer tpcMasterServer = new SocketServer(InetAddress.getLocalHost().getHostAddress(), 8080);
		NetworkHandler tpcMasterHandler = new KVClientHandler(tpcMaster);
		tpcMasterServer.addHandler(tpcMasterHandler);
		tpcMasterServer.connect();
		System.out.println(" * done");
		
		Thread me = (new Thread() {
			public void run() {
				try {
					System.out.println("Starting Master"); 
					tpcMasterServer.run();					
				} catch (Exception e) {
					fail("TPC Master Exception");
				}
			}
		});
		threads.add(me);
		me.start();
		
		Thread.sleep(250);
		
		slaveServers.clear();
		for (int i=0; i<3; i++) {
			long slaveId = slaveServers.size() * 100;
			
			// Create TPCMasterHandler
			System.out.println("Binding SlaveServer:");
			KVServer keyServer = new KVServer(100, 10);
			slaveServers.add(keyServer);
			
			final SocketServer keyServerSocket = new SocketServer(InetAddress.getLocalHost().getHostAddress());
			slaveServers.add(keyServer);
			TPCMasterHandler keyServerHandler = new TPCMasterHandler(keyServer, slaveId);
			keyServerSocket.addHandler(keyServerHandler);
			keyServerSocket.connect();

			// Create TPCLog
			String logPath = slaveId + "@" + keyServerSocket.getHostname();
			TPCLog tpcLog = new TPCLog(logPath, keyServer);
			// Set log for TPCMasterHandler
			keyServerHandler.setTPCLog(tpcLog);
			
			System.out.println(" * Register with master");
			// Register with the Master. Assuming it always succeeds (not catching).
			keyServerHandler.registerWithMaster(masterHostName, keyServerSocket);
			System.out.println(" * done");
			
			me = (new Thread() {
				public void run() {
					try {
						System.out.println("Starting SlaveServer at " + keyServerSocket.getHostname() + ":" + keyServerSocket.getPort());
						keyServerSocket.run();					
					} catch (Exception e) {
						fail("TPC KV Store Slave Exception");
					}
					
				}
			});
			threads.add(me);
			me.start();
		}
	}

	@SuppressWarnings("deprecation")
	@After
	public void tearDown() {
		for (Thread t:threads) {
			t.stop();
		}
	}

	@Test
	public void testHandleGet() throws KVException {
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "val1"), true);
	}

}

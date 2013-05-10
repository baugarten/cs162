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
		
		Thread.sleep(100);
		
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
		Thread.sleep(100);
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
		String resp;
		// try invalid get
		System.out.println("try invalid get");
		try {
			tpcMaster.handleGet(
				new KVMessage("getreq", "key1", null));
			fail("get should throw exception");
		} catch (KVException e) {
			KVMessage err = e.getMsg();
			assertEquals("resp", err.getMsgType());
			assertEquals("Does not exist", err.getMessage());
		}
		
		// try invalid del
		System.out.println("try invalid del");
		try {
			tpcMaster.performTPCOperation(
				new KVMessage("delreq", "key1", null), false);
			fail("del should throw exception");
		} catch (KVException e) {
			KVMessage err = e.getMsg();
			assertEquals("resp", err.getMsgType());
			assertEquals("@0:=Does not exist\n@200:=Does not exist", err.getMessage());
		}

		// put key into store and verify backing store
		System.out.println("try put");
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "val1"), true);
		assertEquals("val1", slaveServers.get(2).get("key1"));
		assertEquals("val1", slaveServers.get(0).get("key1"));
		
		// try cached get
		System.out.println("try put: cached get");
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		// and networked get
		System.out.println("try put: networked get");
		tpcMaster.flushCache();
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		
		// test with two keys
		System.out.println("try put second key");
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key2", "val2"), true);
		assertEquals("val1", slaveServers.get(2).get("key1"));
		assertEquals("val1", slaveServers.get(0).get("key1"));
		assertEquals("val2", slaveServers.get(0).get("key2"));
		assertEquals("val2", slaveServers.get(1).get("key2"));
		
		// try cached gets
		System.out.println("try put second key: cached get");
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));
		// and networked gets
		System.out.println("try put second key: networked get");
		tpcMaster.flushCache();
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));		
		
		// try cached overwrite
		System.out.println("try cached overwrite");
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "overwrite"), true);
		assertEquals("overwrite", slaveServers.get(2).get("key1"));
		assertEquals("overwrite", slaveServers.get(0).get("key1"));
		assertEquals("val2", slaveServers.get(0).get("key2"));
		assertEquals("val2", slaveServers.get(1).get("key2"));
		
		// ensure cache consistent
		System.out.println("try cached overwrite: cached get");
		assertEquals("overwrite",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));
		// and try networked operations
		System.out.println("try cached overwrite: networked get");
		tpcMaster.flushCache();
		assertEquals("overwrite",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));
		
		// try non-cached overwrite
		System.out.println("try non-cached overwrite");
		tpcMaster.flushCache();
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "over2"), true);
		assertEquals("over2", slaveServers.get(2).get("key1"));
		assertEquals("over2", slaveServers.get(0).get("key1"));
		assertEquals("val2", slaveServers.get(0).get("key2"));
		assertEquals("val2", slaveServers.get(1).get("key2"));
		
		// ensure cache consistent
		System.out.println("try non-cached overwrite: cached get");
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));
		// and try networked operations
		System.out.println("try non-cached overwrite: networked get");
		tpcMaster.flushCache();
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "key2", null)));
		
		// try cached delete
		System.out.println("try cached delete");
		tpcMaster.performTPCOperation(
				new KVMessage("delreq", "key2", null), false);
		try{
			slaveServers.get(0).get("key2");
			fail("get should throw exception");
		} catch (KVException e) {}
		try{
			slaveServers.get(1).get("key2");
			fail("get should throw exception");
		} catch (KVException e) {}
		assertEquals("over2", slaveServers.get(2).get("key1"));
		assertEquals("over2", slaveServers.get(0).get("key1"));
		
		// and try doing get
		System.out.println("try cached delete: cached get");
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "key2", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		// and with flushed cache
		System.out.println("try cached delete: networked get");
		tpcMaster.flushCache();
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "key2", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		
		// and repeat with the other one, non-cached delete
		System.out.println("try non-cached delete");
		tpcMaster.flushCache();
		tpcMaster.performTPCOperation(
				new KVMessage("delreq", "key1", null), false);
		try{
			slaveServers.get(2).get("key1");
			fail("get should throw exception");
		} catch (KVException e) {}
		try{
			slaveServers.get(0).get("key1");
			fail("get should throw exception");
		} catch (KVException e) {}
		
		// and try doing get
		System.out.println("try non-cached delete: cached get");
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "key1", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		// and with flushed cache
		System.out.println("try non-cached delete: networked get");
		tpcMaster.flushCache();
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "key1", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		
	}

}

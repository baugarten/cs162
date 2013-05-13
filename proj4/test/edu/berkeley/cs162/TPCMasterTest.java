package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class TPCMasterTest {
	static TPCMaster tpcMaster;
	static ArrayList<KVServer> slaveServers = new ArrayList<KVServer>();
	static ArrayList<SocketServer> slaveSockets = new ArrayList<SocketServer>();
	static ArrayList<TPCMasterHandler> slaveHandlers = new ArrayList<TPCMasterHandler>();
	
	static ArrayList<Thread> threads = new ArrayList<Thread>();
	
	@Before
	public void setUp() throws Exception {
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
			long slaveId = slaveServers.size() * 20000000000000000l;
			
			// Create TPCMasterHandler
			System.out.println("Binding SlaveServer:");
			KVServer keyServer = new KVServer(100, 10);
			slaveServers.add(keyServer);
			
			final SocketServer keyServerSocket = new SocketServer(InetAddress.getLocalHost().getHostAddress());
			slaveSockets.add(keyServerSocket);
			TPCMasterHandler keyServerHandler = new TPCMasterHandler(keyServer, slaveId);
			slaveHandlers.add(keyServerHandler);
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
		// try failures
		// put vote failure
		System.out.println("check put vote failure");
		slaveIgnore(0);
		try{
			tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valz"), true);
			fail("put should throw exception");
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			assertEquals("resp", m.getMsgType());
			assertEquals("@0:=IgnoreNext Error: SlaveServer 0 has ignored this 2PC request during the first phase",
					m.getMessage());
		}
		slaveIgnore(1);
		try{
			tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valz"), true);
			fail("put should throw exception");
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			assertEquals("resp", m.getMsgType());
			assertEquals("@20000000000000000:=IgnoreNext Error: SlaveServer 20000000000000000 has ignored this 2PC request during the first phase",
					m.getMessage());
		}
		slaveIgnore(0);
		slaveIgnore(1);
		try{
			tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valz"), true);
			fail("put should throw exception");
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			assertEquals("resp", m.getMsgType());
			assertEquals("@0:=IgnoreNext Error: SlaveServer 0 has ignored this 2PC request during the first phase\n"
					+ "@20000000000000000:=IgnoreNext Error: SlaveServer 20000000000000000 has ignored this 2PC request during the first phase",
					m.getMessage());
		}
		assertEquals(null, slaveServers.get(0).get("keyz"));
		assertEquals(null, slaveServers.get(1).get("keyz"));
		
		System.out.println("check put commit failure");
		slaveIgnoreCommit(0);
		tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valx"), true);
		assertEquals("valx", slaveServers.get(0).get("keyz"));
		assertEquals("valx", slaveServers.get(1).get("keyz"));

		slaveIgnoreCommit(1);
		tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valy"), true);
		assertEquals("valy", slaveServers.get(0).get("keyz"));
		assertEquals("valy", slaveServers.get(1).get("keyz"));
		
		slaveIgnoreCommit(0);
		slaveIgnoreCommit(1);
		tpcMaster.performTPCOperation(new KVMessage("putreq", "keyz", "valz"), true);
		assertEquals("valz", slaveServers.get(0).get("keyz"));
		assertEquals("valz", slaveServers.get(1).get("keyz"));
		
		System.out.println("check single get failure");
		tpcMaster.flushCache();
		slaveIgnoreCommit(0);
		assertEquals("valz",
				tpcMaster.handleGet(new KVMessage("getreq", "keyz", null)));

		tpcMaster.flushCache();
		slaveIgnoreCommit(1);
		assertEquals("valz",
				tpcMaster.handleGet(new KVMessage("getreq", "keyz", null)));
		
		System.out.println("check multiple get failure");
		tpcMaster.flushCache();
		slaveIgnoreCommit(0);
		slaveIgnoreCommit(1);
		try {
			tpcMaster.handleGet(new KVMessage("getreq", "keyz", null));
			fail("put should throw exception on both failures");
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			assertEquals("resp", m.getMsgType());
			assertEquals("@0:=Network Error: Could not receive data\n"
					+ "@20000000000000000:=Network Error: Could not receive data",
					m.getMessage());
		}
		
		System.out.println("check del abort failure");
		slaveIgnore(0);
		try{
			tpcMaster.performTPCOperation(new KVMessage("delreq", "keyz", null), false);
			fail("put should throw exception");
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			assertEquals("resp", m.getMsgType());
			assertEquals("@0:=IgnoreNext Error: SlaveServer 0 has ignored this 2PC request during the first phase",
					m.getMessage());
		}
		assertEquals("valz", slaveServers.get(0).get("keyz"));
		assertEquals("valz", slaveServers.get(1).get("keyz"));

		System.out.println("check put commit failure");
		slaveIgnoreCommit(0);
		slaveIgnoreCommit(1);
		tpcMaster.performTPCOperation(new KVMessage("delreq", "keyz", null), false);
		assertEquals(null, slaveServers.get(0).get("keyz"));
		assertEquals(null, slaveServers.get(1).get("keyz"));
		
		// check backing store
		System.out.println("check initial backing store");
		assertEquals(null, slaveServers.get(0).get("key1"));
		assertEquals(null, slaveServers.get(1).get("key1"));
		assertEquals(null, slaveServers.get(2).get("a"));
		assertEquals(null, slaveServers.get(0).get("a"));
		
		// try invalid get
		System.out.println("try invalid get");
		try {
			tpcMaster.handleGet(
				new KVMessage("getreq", "key1", null));
			fail("get should throw exception");
		} catch (KVException e) {
			KVMessage err = e.getMsg();
			assertEquals("resp", err.getMsgType());
			assertEquals("@0:=Does not exist\n@20000000000000000:=Does not exist", err.getMessage());
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
			assertEquals("@0:=Does not exist\n@20000000000000000:=Does not exist", err.getMessage());
		}

		// put key into store and verify backing store
		System.out.println("try put");
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "val1"), true);
		assertEquals("val1", slaveServers.get(0).get("key1"));
		assertEquals("val1", slaveServers.get(1).get("key1"));
		
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
				new KVMessage("putreq", "a", "val2"), true);
		assertEquals("val1", slaveServers.get(0).get("key1"));
		assertEquals("val1", slaveServers.get(1).get("key1"));
		assertEquals("val2", slaveServers.get(2).get("a"));
		assertEquals("val2", slaveServers.get(0).get("a"));
		
		// try cached gets
		System.out.println("try put second key: cached get");
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));
		// and networked gets
		System.out.println("try put second key: networked get");
		tpcMaster.flushCache();
		assertEquals("val1",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));		
		
		// try cached overwrite
		System.out.println("try cached overwrite");
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "overwrite"), true);
		assertEquals("overwrite", slaveServers.get(0).get("key1"));
		assertEquals("overwrite", slaveServers.get(1).get("key1"));
		assertEquals("val2", slaveServers.get(2).get("a"));
		assertEquals("val2", slaveServers.get(0).get("a"));
		
		// ensure cache consistent
		System.out.println("try cached overwrite: cached get");
		assertEquals("overwrite",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));
		// and try networked operations
		System.out.println("try cached overwrite: networked get");
		tpcMaster.flushCache();
		assertEquals("overwrite",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));
		
		// try non-cached overwrite
		System.out.println("try non-cached overwrite");
		tpcMaster.flushCache();
		tpcMaster.performTPCOperation(
				new KVMessage("putreq", "key1", "over2"), true);
		assertEquals("over2", slaveServers.get(0).get("key1"));
		assertEquals("over2", slaveServers.get(1).get("key1"));
		assertEquals("val2", slaveServers.get(2).get("a"));
		assertEquals("val2", slaveServers.get(0).get("a"));
		
		// ensure cache consistent
		System.out.println("try non-cached overwrite: cached get");
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));
		// and try networked operations
		System.out.println("try non-cached overwrite: networked get");
		tpcMaster.flushCache();
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		assertEquals("val2",
				tpcMaster.handleGet(new KVMessage("getreq", "a", null)));
		
		// try cached delete
		System.out.println("try cached delete");
		tpcMaster.performTPCOperation(
				new KVMessage("delreq", "a", null), false);
		assertEquals(null, slaveServers.get(2).get("a"));
		assertEquals(null, slaveServers.get(0).get("a"));
		assertEquals("over2", slaveServers.get(0).get("key1"));
		assertEquals("over2", slaveServers.get(1).get("key1"));
		
		// and try doing get
		System.out.println("try cached delete: cached get");
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "a", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		// and with flushed cache
		System.out.println("try cached delete: networked get");
		tpcMaster.flushCache();
		assertEquals("over2",
				tpcMaster.handleGet(new KVMessage("getreq", "key1", null)));
		try{
			tpcMaster.handleGet(new KVMessage("getreq", "a", null));
			fail("get should throw exception");
		} catch (KVException e) {}
		
		// and repeat with the other one, non-cached delete
		System.out.println("try non-cached delete");
		tpcMaster.flushCache();
		tpcMaster.performTPCOperation(
				new KVMessage("delreq", "key1", null), false);
		
		assertEquals(null, slaveServers.get(0).get("key1"));
		assertEquals(null, slaveServers.get(1).get("key1"));
		
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
	
	void slaveIgnoreCommit(int slave) {
		slaveHandlers.get(slave).ignoreNextCommit = true;
	}
	void slaveIgnore(int slave) {
		try {
			SocketServer serv = slaveSockets.get(slave);
			Socket sock = new Socket(serv.getHostname(), serv.getPort());
			sock.setSoTimeout(2500);
			System.out.println("ignoreNext => " + sock.toString());
			
			KVMessage msg = new KVMessage("ignoreNext"); 
			msg.sendMessage(sock);
			
			KVMessage response = new KVMessage(sock.getInputStream());
			sock.close();
			assertEquals("resp", response.getMsgType());
			assertEquals("Success", response.getMessage());
		} catch (KVException e) {
			KVMessage m = e.getMsg();
			fail("slaveIgnore KVException: " + m.getMsgType() + " " + m.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			fail("slaveIgnore exception: " + e);
		}
	}

}

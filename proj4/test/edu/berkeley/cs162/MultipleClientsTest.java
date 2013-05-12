package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MultipleClientsTest {
	static TPCMaster tpcMaster;
	static ArrayList<KVServer> slaveServers = new ArrayList<KVServer>();
	static ArrayList<SocketServer> slaveSockets = new ArrayList<SocketServer>();
	static ArrayList<TPCMasterHandler> slaveHandlers = new ArrayList<TPCMasterHandler>();
	static ArrayList<KVClient> clients = new ArrayList<KVClient>();
	
	static ArrayList<Thread> threads = new ArrayList<Thread>();
	static ArrayList<Thread> clientThreads = new ArrayList<Thread>();
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
		NetworkHandler tpcMasterHandler = new KVClientHandler(3, tpcMaster);
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
		
		Thread.sleep(1000);
		
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
		Thread.sleep(1000);
		
	}

	@SuppressWarnings("deprecation")
	@After
	public void tearDown() throws Exception {
		for (Thread t:threads) {
			t.stop();
		}
		for (Thread t:clientThreads) {
			t.stop();
		}
	}

	@Test
	public void testMultipleClient() throws InterruptedException {
		Thread me = null;
		final Semaphore masterThread = new Semaphore(-2);
		
		//client 1
		me = (new Thread(){
			public void run(){
				String name = "KVClient1";
				try {
					KVClient kc = new KVClient(InetAddress.getLocalHost().getHostAddress(),8080);
					
					System.out.println("Starting" + name);
					
					String three = "three";
					String seven = "seven";
					System.out.println(name + ": putting (3, 7)");
					kc.put(three, seven);
					
					System.out.println(name + ": putting (3, 7) again");
					kc.put(three, seven);
					
					System.out.println(name + ": getting key = 3");			
					String value = kc.get(three);					
					assertEquals("seven",value);
					
					System.out.println(name + ": deleting key = 3");
					kc.del(three);
					
					// suppose to throw exception because deletet non-existent key
					kc.get(three);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				} catch (IOException e1){
					e1.printStackTrace();
				}
				masterThread.release();
			}
		});
		
		clientThreads.add(me);
		me.start();
		
		//client 2
		me = (new Thread(){
			public void run(){
				String name = "KVClient2";
				try {
					KVClient kc = new KVClient(InetAddress.getLocalHost().getHostAddress(),8080);
					
					System.out.println("Starting " + name);
					
					String keyz = "keyz";
					String valuez = "valuez";
					System.out.println(name + ": putting (keyz, valuez)");
					kc.put(keyz, valuez);
					
					System.out.println(name + ": putting (kez, valuez) again");
					kc.put(keyz, valuez);
					
					System.out.println(name + ": getting key = keyz");			
					String value = kc.get(keyz);					
					assertEquals("valuez",valuez);
					
					System.out.println(name + ": deleting key = keyz");					
					kc.del(keyz);
					
					// suppose to throw exception because deletet non-existent key
					kc.get(keyz);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				} catch (IOException e1){
					e1.printStackTrace();
				}
				masterThread.release();
			}
			
		});
		clientThreads.add(me);
		me.start();
		
		//Crazy client
		final String overSizedKey = overSizedKey();
		final String overSizedValue = overSizedValue();
		me = (new Thread(){
			public void run(){
				String name = "CrazyClient";
				String keyz = "OyOyOy";
				String valuez = "valuez";
				KVClient kc = null;
				
				try {
					kc = new KVClient(InetAddress.getLocalHost().getHostAddress(),8080);
				} catch (UnknownHostException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
					return;
				}

				try{
					System.out.println(name + ": deletingnon-existent key = " + keyz);					
					kc.del(keyz);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				}
				
				try {				
					System.out.println(name + ": putting oversized key");
					kc.put(overSizedKey, valuez);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				}
				
				try {		
					System.out.println(name + ": Sudden sane action");
					System.out.println(name + ": putting (CrazyClient, I'M SANE!!!)");
					kc.put("CrazyClient", "I'M SANE");
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
					fail();
				}
				
				try {
					System.out.println(name + ": putting oversized value");
					kc.put(keyz,overSizedValue);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				}
				
				try {
					System.out.println(name + ": getting oversized key");
					kc.get(overSizedKey);
					fail();
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
				}			
				
				try {		
					System.out.println(name + ": Sudden sane action");
					System.out.println(name + ": getting key = CrazyClient");
					String result = kc.get("CrazyClient");
					assertEquals("I'M SANE", result);
				} catch (KVException e) {
					System.out.println(name + ": " + e.getMsg().getMessage());
					fail();
				}
				masterThread.release();
			}
		});
		clientThreads.add(me);
		me.start();
	
		masterThread.acquire();
		
		for(Thread client: clientThreads){
			assertEquals(false, client.isAlive());
		}
	}
	
	private String overSizedKey(){
		String result = "";
		for(int i = 0; i < 26; i++){
			result += "HelloWorld";
		}
		return result;
	}
	
	private String overSizedValue(){
		StringBuffer buf = new StringBuffer();
		String subString = "HelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorld";
		for(int i = 0; i<2560; i++){
			buf.append(subString);
		}
		buf.append("Hello");
		return buf.toString();
	}
}

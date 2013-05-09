package test.edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.berkeley.cs162.KVCache;
import edu.berkeley.cs162.KVClient;
import edu.berkeley.cs162.KVClientHandler;
import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.KVStore;
import edu.berkeley.cs162.NetworkHandler;
import edu.berkeley.cs162.SocketServer;

public class KVClientHandlerTest {

	private static KVClientHandler kvClientHandler;
	private Socket throwingSocket;
	private Socket socket;
	private PipedInputStream in;
	private static Thread t;
	
	private static volatile KVServer kvServer;
	
	private static volatile KVCache kvCache;

	private static volatile KVStore kvStore;
	
	private static volatile SocketServer socketServer;
	
	@Before
	public void setUp() throws Exception {
		final Runnable serversetup = new Runnable() {
			public void run() {
				synchronized (this) {
					kvServer =  new KVServer(4, 20);
					kvCache = kvServer.getCache();
					kvStore = kvServer.getStore();
					
					socketServer = new SocketServer("localhost", 8080);
					NetworkHandler handler = new KVClientHandler(kvServer);
					
					socketServer.addHandler(handler);
					
					try {
						socketServer.connect();
						this.notify();
						socketServer.run();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		t = new Thread(serversetup);
		t.start();
		synchronized(serversetup) {
			serversetup.wait();
		}
	}
	
	@After
	public void tearDownServer() throws Exception {
		t.stop();
		socketServer.stop();
		kvServer = null;
		socketServer = null;
	}
	
	@Test
	public void testFailingConnection() throws Exception {
		kvClientHandler.handle(throwingSocket);
		try {
			KVMessage result = new KVMessage(throwingSocket.getInputStream());
			fail();
		} catch (KVException e) {
			assertEquals("Network Error: Could not receive data", e.getMsg().getMessage());
		}
	}
}

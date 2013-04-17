package test.berkeley.cs162;

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

import edu.berkeley.cs162.KVClientHandler;
import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.SocketServer;

public class KVClientHandlerTest {

	private static KVServer kvServer;
	private static KVClientHandler kvClientHandler;
	private Socket throwingSocket;
	private Socket socket;
	private PipedInputStream in;
	private static Thread t;
	private static SocketServer server;
	
	@Before
	public void setUp() throws Exception {
		throwingSocket = new UnreadableSocket();
		in = new PipedInputStream();
		socket = new Socket("localhost", 3014);
		System.out.println(socket.isConnected());
	}
	@BeforeClass
	public static void setUpServer() throws Exception {
		kvServer = new KVServer(4, 20);
		kvClientHandler = new KVClientHandler(kvServer);
		server = new SocketServer("localhost", 3014);
		server.addHandler(new KVClientHandler(kvServer));
		server.connect();
		Thread t = new Thread() {
			public void run() {
				try {
					server.run();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			public void destroy() {
				server.stop();
			}
		};
		t.start();
	}
	
	@AfterClass
	public static void tearDownServer() throws Exception {
		System.out.println("Stopping server");
		t.destroy();
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
	
	@Test
	public void testConnection() throws Exception {
		KVMessage inp = new KVMessage("getreq");
		inp.setKey("key");
		inp.sendMessage(socket);
		//kvClientHandler.handle(socket);
		System.out.println("Reading res");
		KVMessage res = new KVMessage(socket.getInputStream());
		System.out.println("Read res");
		assertEquals("resp", res.getMsgType());
		assertEquals("Does not exist", res.getMessage());
		System.out.println("Done");
	}
}

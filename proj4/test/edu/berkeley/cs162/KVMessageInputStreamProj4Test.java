package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.*;

public class KVMessageInputStreamProj4Test {
	
	private KVMessage kvMessage;
	private PipedOutputStream out;
	private PipedInputStream pipedIn;
	private PipedOutputStream pipedOut;

	@Before
	public void setUp() throws Exception {
		pipedOut = new PipedOutputStream();
		pipedIn = new PipedInputStream(pipedOut); 
		out = pipedOut;
	}

	@After
	public void tearDown() throws Exception {
		out.close();
		pipedIn.close();
	}

	@Test
	public void testKVMessageInputStreamValidTPCPut() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value>Bear</Value><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("putreq", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
		assertEquals("Bear", kvMessage.getValue());
		assertEquals("9", kvMessage.getTpcOpId());
	}

	@Test
	public void testKVMessageInputStreamValidTPCDel() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"delreq\"><Key>Cal</Key><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("delreq", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	@Test
	public void testKVMessageInputStreamValidReady() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"ready\"><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("ready", kvMessage.getMsgType());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	@Test
	public void testKVMessageInputStreamValidSlaveAbort() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"abort\"><Message>I'm sleeping</Message><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("abort", kvMessage.getMsgType());
		assertEquals("I'm sleeping", kvMessage.getMessage());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	@Test
	public void testKVMessageInputStreamValidCommit() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"commit\"><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("commit", kvMessage.getMsgType());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	@Test
	public void testKVMessageInputStreamValidGlobalAbort() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"abort\"><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("abort", kvMessage.getMsgType());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	@Test
	public void testKVMessageInputStreamValidTPCAck() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"ack\"><TPCOpId>9</TPCOpId></KVMessage>";
		out.write(request.getBytes("UTF-8"));
		kvMessage = new KVMessage(pipedIn);
		assertEquals("ack", kvMessage.getMsgType());
		assertEquals("9", kvMessage.getTpcOpId());
	}
	
	//Error: Ready without TPCOpId
	@Test
	public void testKVMessageInputStreamBadXMLFormat1() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"ready\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	//Error: Commit without TPCOpId
	@Test
	public void testKVMessageInputStreamBadXMLFormat2() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"commit\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	//Error: Abort without TPCOpId
	@Test
	public void testKVMessageInputStreamBadXMLFormat3() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"abort\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	//Error: Ack without TPCOpId
	@Test
	public void testKVMessageInputStreamBadXMLFormat4() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"ack\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	//Error: Register without Message
	@Test
	public void testKVMessageInputStreamBadXMLFormat5() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"register\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	//Error: Response without Message
	@Test
	public void testKVMessageInputStreamBadXMLFormat6() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"resp\"><Key>Cal</Key></KVMessage>";
			out.write(request.getBytes("UTF-8"));
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
		
}

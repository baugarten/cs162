package test.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;


//import edu.berkeley.cs162.KVMessage.NoCloseInputStream;

public class KVMessageInputStreamTest {

	private KVMessage kvMessage;
	private ObjectOutputStream out;
	private PipedInputStream pipedIn;
	private PipedOutputStream pipedOut;

	@Before
	public void setUp() throws Exception {
		pipedOut = new PipedOutputStream();
		pipedIn = new PipedInputStream(pipedOut); 
		out = new ObjectOutputStream(pipedOut);
	}

	@After
	public void tearDown() throws Exception {
		out.close();
		pipedIn.close();
	}

	@Test
	public void testKVMessageInputStreamValidGet() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
		out.writeObject(request);
		kvMessage = new KVMessage(pipedIn);
		assertEquals("getreq", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
	}
	
	@Test
	public void testKVMessageInputStreamValidPut() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value>Bear</Value></KVMessage>";
		out.writeObject(request);
		kvMessage = new KVMessage(pipedIn);
		assertEquals("putreq", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
		assertEquals("Bear", kvMessage.getValue());
	}
	
	@Test
	public void testKVMessageInputStreamValidDel() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"delreq\"><Key>Cal</Key></KVMessage>";
		out.writeObject(request);
		kvMessage = new KVMessage(pipedIn);
		assertEquals("delreq", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
	}
	
	@Test
	public void testKVMessageInputStreamValidResp1() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"resp\"><Key>Cal</Key><Value>Bear</Value></KVMessage>";
		out.writeObject(request);
		kvMessage = new KVMessage(pipedIn);
		assertEquals("resp", kvMessage.getMsgType());
		assertEquals("Cal", kvMessage.getKey());
		assertEquals("Bear", kvMessage.getValue());
	}
	
	@Test
	public void testKVMessageInputStreamValidResp2() throws KVException,IOException {
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"resp\"><Message>Success</Message></KVMessage>";
		out.writeObject(request);
		kvMessage = new KVMessage(pipedIn);
		assertEquals("resp", kvMessage.getMsgType());
		assertEquals("Success", kvMessage.getMessage());
	}
	
	//Error: unparseable XML
	@Test
	public void testKVMessageInputStreamBadXML() throws IOException {
		try {
			String request = "< version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("XML Error: Received unparseable message", e.getMsg()
					.getMessage());
		}  
	}
	
	//Error: Root node's name is invalid
	@Test
	public void testKVMessageInputStreamBadXMLFormat1() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KV type=\"getreq\"><Key>Cal</Key></KV>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}
	
	
	//Error: No value node in a putreq
	@Test
	public void testKVMessageInputStreamBadXMLFormat2() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"putreq\"><Key>Cal</Key></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}

	//Error: Value node's name is invalid
	@Test
	public void testKVMessageInputStreamBadXMLFormat3() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"putreq\"><Key>Cal</Key><Val>Bear</Val></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}

	//Error: Missing value node in a getreq response
	@Test
	public void testKVMessageInputStreamBadXMLFormat4() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"resp\"><Key>Cal</Key></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}

	//Error: Has more than one message in other kinds of response
	@Test
	public void testKVMessageInputStreamBadXMLFormat5() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"resp\"><Message>message1</Message><Message>message2</Message></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}

	//Error: Has key node in error or success response
	@Test
	public void testKVMessageInputStreamBadXMLFormat6() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"resp\"><Key>Cal</Key><Message>Success</Message></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Message format incorrect", e.getMsg().getMessage());
		}
	}

	//Error: key with size 0
	@Test
	public void testKVMessageInputStreamBadXMLFormat7() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"getreq\"><Key></Key></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Undersized key", e.getMsg().getMessage());
		}
	}

	//Error: value with size 0
	@Test
	public void testKVMessageInputStreamBadXMLFormat8() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value></Value></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			fail();
		} catch (KVException e){
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals("Undersized value", e.getMsg().getMessage());
		}
	}
	
	//Ignore the extra value node in a getreq
	@Test
	public void testKVMessageInputStreamOkToIgnore1() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"getreq\"><Key>Cal</Key><Value></Value></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			assertEquals("getreq",kvMessage.getMsgType());
			assertEquals("Cal",kvMessage.getKey());
			assertEquals(null,kvMessage.getValue());
			assertEquals(null,kvMessage.getMessage());
		} catch (KVException e){
			fail();
		}
	}
	
	//Ignore the extra value node, message node, and garbage node in a delreq
	@Test
	public void testKVMessageInputStreamOkToIgnore2() throws IOException {
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"delreq\"><Key>Cal</Key><Value>Bear</Value><Message>Golden</Message><Booo>Stanford</Booo></KVMessage>";
			out.writeObject(request);
			kvMessage = new KVMessage(pipedIn);
			assertEquals("delreq",kvMessage.getMsgType());
			assertEquals("Cal",kvMessage.getKey());
			assertEquals(null,kvMessage.getValue());
			assertEquals(null,kvMessage.getMessage());
		} catch (KVException e){
			fail();
		}
	}	
}

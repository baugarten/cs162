package test.edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PipedInputStream;
import java.net.Socket;

import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;

public class KVMessageSendMessageTest {

	private KVMessage kvMessage, received;
	private PipedInputStream in;
	private StubSocket socket;
	
	@Before
	public void setUp(){
		in = new PipedInputStream();
		try {
			socket = new StubSocket(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void testSendMessage(){
		try {
			String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			request += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
			kvMessage = new KVMessage("getreq");
			kvMessage.setKey("Cal");
			kvMessage.sendMessage(socket);
			received = new KVMessage(in);
			assertEquals(request,received.toXML());
			socket.close();
		} catch (Exception e) {
			fail();
		}
	}
	
	//Error: can not send with closed socket
	@Test
	public void testSendMessageWithClosedSocket() throws IOException{
		try{
			Socket closeSocket = new Socket();
			closeSocket.close();
			kvMessage = new KVMessage("getreq");
			kvMessage.setKey("Cal");
			kvMessage.sendMessage(closeSocket);
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Network Error: Could not send data",e.getMsg().getMessage());
		}
	}

}

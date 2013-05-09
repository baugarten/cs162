package test.edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;

public class KVMessageStringTest {
	
	private KVMessage kvMessage;
	
	@Test
	public void testKVMessageStringPutReq() {
		String msgType = "putreq";
		try {
			kvMessage = new KVMessage(msgType);
			assertEquals("putreq",kvMessage.getMsgType());
		} catch (KVException e){
			System.out.println(e.getMsg().getMessage());
		}

	}

	@Test
	public void testKVMessageStringGetReq() {
		String msgType = "getreq";
		try {
			kvMessage = new KVMessage(msgType);
			assertEquals("getreq",kvMessage.getMsgType());
		} catch (KVException e){
			System.out.println(e.getMsg().getMessage());
		}

	}
	@Test
	public void testKVMessageStringDelReq() {
		String msgType = "delreq";
		try {
			kvMessage = new KVMessage(msgType);
			assertEquals("delreq",kvMessage.getMsgType());
		} catch (KVException e){
			System.out.println(e.getMsg().getMessage());
		}

	}
	
	@Test
	public void testKVMessageStringResp() {
		String msgType = "resp";
		try {
			kvMessage = new KVMessage(msgType);
			assertEquals("resp",kvMessage.getMsgType());
		} catch (KVException e){
			System.out.println(e.getMsg().getMessage());
		}

	}
	
	@Test(expected=KVException.class)
	public void testKVMessageStringBadType() throws KVException{
		String msgType = "hello";
		new KVMessage(msgType);
		fail();
	}
	
	

}

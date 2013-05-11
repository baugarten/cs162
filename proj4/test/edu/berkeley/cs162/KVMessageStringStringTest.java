package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;

public class KVMessageStringStringTest {

	private KVMessage kvMessage;
	
	@Test
	public void testKVMessageStringStringResp() throws KVException {
		String msgType = "resp";
		String message = "Success";
		kvMessage = new KVMessage(msgType,message);
		assertEquals("resp",kvMessage.getMsgType());
		assertEquals("Success",kvMessage.getMessage());
	}
	
	
	@Test(expected=KVException.class)
	public void testKVMessageStringStringBadType() throws KVException{
		String msgType = "Hello";
		String message = "Success";
		kvMessage = new KVMessage(msgType,message);
		fail();
	}

}

package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KVMessageToXMLTest {
	
	private KVMessage kvMessage, kvMessagePut;

	@Before
	public void setUp() throws Exception {
		kvMessage = new KVMessage("getreq");
		kvMessagePut = new KVMessage("putreq");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testToXMLGetReq() throws KVException{
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		expected += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
		kvMessage.setKey("Cal");
		String result = kvMessage.toXML();
		assertEquals(expected,result);
	}
	
	
	@Test
	public void testToXMLNotEnoughData() {
		try {
			kvMessage.setKey(null);
			kvMessage.toXML();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}
	
	/*
	@Test
	public void testToXMLOversizedKey() {
		try{
			String key = "";
			for(int i = 0; i < 30; i++){
				key += "HelloWorld";
			}
			kvMessage.setKey(key);
			kvMessage.toXML();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Oversized key",e.getMsg().getMessage());
		} finally {
			kvMessage.setKey(null);
		}
	}
	
	@Test
	public void testToXMLOversizedValue() {
		try{
			String value = "";
			for(int i = 0; i < 30000; i++){
				value += "HelloWorld";
			}
			kvMessagePut.setKey("Cal");
			kvMessagePut.setValue(value);
			kvMessagePut.toXML();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Oversized value",e.getMsg().getMessage());
		} finally {
			kvMessagePut.setKey(null);
		}
	}
	*/
}

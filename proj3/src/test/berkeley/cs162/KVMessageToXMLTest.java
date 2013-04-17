package test.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;

public class KVMessageToXMLTest {
	
	private KVMessage kvMessage;
	private String result;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testToXMLGetReq() throws KVException{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
		kvMessage = new KVMessage("getreq");
		kvMessage.setKey("Cal");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLPutReq() throws KVException{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value>Bear</Value></KVMessage>";
		kvMessage = new KVMessage("putreq");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLDelReq() throws KVException{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"delreq\"><Key>Cal</Key></KVMessage>";
		kvMessage = new KVMessage("delreq");
		kvMessage.setKey("Cal");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLGetResp() throws KVException{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"resp\"><Key>Cal</Key><Value>Bear</Value></KVMessage>";
		kvMessage = new KVMessage("resp");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLPutDelErrorResp() throws KVException{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"resp\"><Message>Success</Message></KVMessage>";
		kvMessage = new KVMessage("resp");
		kvMessage.setMessage("Success");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	//Error: get request with no key
	@Test
	public void testToXMLNotEnoughData1() {
		try {
			kvMessage = new KVMessage("getreq");
			kvMessage.setKey(null);
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}

	//Error: put request with no value
	@Test
	public void testToXMLNotEnoughData2() {
		try {
			kvMessage = new KVMessage("putreq");
			kvMessage.setKey("Cal");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}

	//Error: del request with no key
	@Test
	public void testToXMLNotEnoughData3() {
		try {
			kvMessage = new KVMessage("delreq");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}
	
	//Error: get response with no key
	@Test
	public void testToXMLNotEnoughData5() {
		try {
			kvMessage = new KVMessage("resp");
			kvMessage.setValue("Bear");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}	
	
	//Error: get response with no value
	@Test
	public void testToXMLNotEnoughData6() {
		try {
			kvMessage = new KVMessage("resp");
			kvMessage.setKey("Cal");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}
	

	//Error: get response with a message
	@Test
	public void testToXMLNotEnoughData7() {
		try {
			kvMessage = new KVMessage("resp");
			kvMessage.setKey("Cal");
			kvMessage.setValue("Bear");
			kvMessage.setMessage("Golden");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}

	
	//Error: del, put, or error response with a key
	@Test
	public void testToXMLNotEnoughData8() {
		try {
			kvMessage = new KVMessage("resp");
			kvMessage.setKey("Cal");
			kvMessage.setMessage("Success");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}
	
	//Error: del, put, or error response with a value
	@Test
	public void testToXMLNotEnoughData9() {
		try {
			kvMessage = new KVMessage("resp");
			kvMessage.setValue("Bear");
			kvMessage.setMessage("Success");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Unknown Error: Not enough data available to generate a valid XML message",
					e.getMsg().getMessage());
		}
	}	

	//Error: key with size 0
	@Test
	public void testToXMLNotEnoughData10() {
		try {
			kvMessage = new KVMessage("getreq");
			kvMessage.setKey("");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Undersized key",
					e.getMsg().getMessage());
		}
	}
	
	//Error: value with size 0
	@Test
	public void testToXMLNotEnoughData11() {
		try {
			kvMessage = new KVMessage("putreq");
			kvMessage.setValue("");
			kvMessage.setKey("Cal");
			kvMessage.toXML();
			fail();
		} catch (KVException e) {
			assertEquals("resp", e.getMsg().getMsgType());
			assertEquals(
					"Undersized value",
					e.getMsg().getMessage());
		}
	} 
	
	//Error: oversized key in putreq
	@Test
	public void testToXMLOversizedKey() {
		try{
			String key = "";
			for(int i = 0; i < 30; i++){
				key += "HelloWorld";
			}
			kvMessage = new KVMessage("putreq");
			kvMessage.setKey(key);
			kvMessage.setValue("Bear");
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Oversized key",e.getMsg().getMessage());
		}
	}
	
	//Error: oversized value in putreq
	@Test
	public void testToXMLOversizedValue() {
		try{
			String value = "";
			for(int i = 0; i < 2561; i++){
				value += "HelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorldHelloWorld";
			}
			kvMessage = new KVMessage("putreq");
			kvMessage.setKey("Cal");
			kvMessage.setValue(value);
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Oversized value",e.getMsg().getMessage());
		}
	}
	
	//Ignore value node and message node in getreq
	@Test
	public void testToXMLOkToIgnore1() throws KVException{
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"getreq\"><Key>Cal</Key></KVMessage>";
		kvMessage = new KVMessage("getreq");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		kvMessage.setMessage("Golden");
		assertEquals(request,kvMessage.toXML());
	}
	
	//Ignore message node in putreq
	@Test
	public void testToXMLOkToIgnore2() throws KVException{
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value>Bear</Value></KVMessage>";
		kvMessage = new KVMessage("putreq");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		kvMessage.setMessage("Golden");
		assertEquals(request,kvMessage.toXML());
	}
	
	//Ignore value node and message node in delreq
	@Test
	public void testToXMLOkToIgnore3() throws KVException{
		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		request += "<KVMessage type=\"delreq\"><Key>Cal</Key></KVMessage>";
		kvMessage = new KVMessage("delreq");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		kvMessage.setMessage("Golden");
		assertEquals(request,kvMessage.toXML());
	}
}

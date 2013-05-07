package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KVMessageToXMLProj4Test {
	
	private KVMessage kvMessage;
	private String result;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testToXMLPutTPC() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"putreq\"><Key>Cal</Key><Value>Bear</Value><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("putreq");
		kvMessage.setKey("Cal");
		kvMessage.setValue("Bear");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLDelTPC() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"delreq\"><Key>Cal</Key><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("delreq");
		kvMessage.setKey("Cal");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLReadyTPC() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"ready\"><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("ready");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLCommitTPC() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"commit\"><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("commit");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLGlobalAbort() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"abort\"><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("abort");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLSlaveAbort() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"abort\"><Message>I'm sleeping</Message><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("abort");
		kvMessage.setMessage("I'm sleeping");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}

	@Test
	public void testToXMLAckTPC() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"ack\"><TPCOpId>9</TPCOpId></KVMessage>";
		kvMessage = new KVMessage("ack");
		kvMessage.setTpcOpId("9");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLSlaveReg() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"register\"><Message>1@localhost:60000</Message></KVMessage>";
		kvMessage = new KVMessage("register");
		kvMessage.setMessage("1@localhost:60000");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLAckReg() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"resp\"><Message>Successfully registered 1@localhost:60000</Message></KVMessage>";
		kvMessage = new KVMessage("resp");
		kvMessage.setMessage("Successfully registered 1@localhost:60000");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	@Test
	public void testToXMLIgnoreNext() throws KVException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<KVMessage type=\"ignoreNext\"/>";
		kvMessage = new KVMessage("ignoreNext");
		result = kvMessage.toXML();
		assertEquals(xml,result);
	}
	
	//Error: ready without TPCOpId
	@Test
	public void testToXMLNotEnoughData1() {
		try {
			kvMessage = new KVMessage("ready");
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}
	
	//Error: commit without TPCOpId
	@Test
	public void testToXMLNotEnoughData2() {
		try {
			kvMessage = new KVMessage("commit");
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}
	
	//Error: ack without TPCOpId
	@Test
	public void testToXMLNotEnoughData3() {
		try {
			kvMessage = new KVMessage("ack");
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}
	
	//Error: register without Message
	@Test
	public void testToXMLNotEnoughData4() {
		try {
			kvMessage = new KVMessage("register");
			kvMessage.toXML();
			fail();
		} catch (KVException e){
			assertEquals("resp",e.getMsg().getMsgType());
			assertEquals("Unknown Error: Not enough data available to generate a valid XML message",e.getMsg().getMessage());
		}
	}
}

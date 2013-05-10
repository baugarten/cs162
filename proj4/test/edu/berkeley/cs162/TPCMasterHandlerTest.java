package edu.berkeley.cs162;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TPCMasterHandlerTest {
	private KVServer kvServer;
	private TPCMasterHandler handler;
	
	@Before
	public void setUp() {
		kvServer = new KVServer(10, 2);
		handler = new TPCMasterHandler(kvServer);
	}
	
	@Test
	public synchronized void testGet() throws Exception {
		SpySocket spyerer = new SpySocket();
		KVMessage msg = new KVMessage("putreq");
		msg.setKey("balls");
		msg.setValue("yes");
		msg.setTpcOpId("1");
		spyerer.setKVMessage(msg);
		handler.handle(spyerer);
		while (spyerer.getOutputMessage() == null) {
		}
		KVMessage expected = new KVMessage("ready");
		expected.setTpcOpId("1");
		Assert.assertEquals(expected, spyerer.getOutputMessage());
		
		msg = new KVMessage("getreq");
		msg.setKey("balls");
		spyerer = new SpySocket();
		spyerer.setKVMessage(msg);
		handler.handle(spyerer);
		this.wait(100);
		expected = new KVMessage("resp");
		expected.setKey("balls");
		expected.setValue("yes");
		Assert.assertEquals(expected, spyerer.getOutputMessage());
	}

}

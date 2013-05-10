package edu.berkeley.cs162;

import java.io.IOException;
import java.net.Socket;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TPCMasterHandlerTest {
	private KVServer kvServer;
	private TPCMasterHandler handler;
	
	@Before
	public void setUp() {
		kvServer = new KVServer(4, 20);
		handler = new TPCMasterHandler(kvServer);
		handler.setTPCLog(new TPCLog("log", kvServer));
	}
	
	@Test
	public void testGet() throws Exception {
		SpySocket spyerer = new SpySocket();
		KVMessage msg = new KVMessage("putreq");
		msg.setKey("balls");
		msg.setValue("yes");
		msg.setTpcOpId("1");
		
		KVMessage result = handle(msg);
		if (result == null) {
			Assert.fail("Never received response from server");
		}
		
		KVMessage expected = new KVMessage("ready");
		expected.setTpcOpId("1");
		Assert.assertEquals(expected, result);
		
		msg = new KVMessage("getreq");
		msg.setKey("balls");
		
		result = handle(msg);
		if (result == null) {
			Assert.fail("Never received response from server");
		}
		expected = new KVMessage("resp");
		expected.setMessage("Does not exist");
		Assert.assertEquals(expected, result);
		
		msg = new KVMessage("commit");
		msg.setTpcOpId("1");
		
		result = handle(msg);
		expected = new KVMessage("ack");
		expected.setTpcOpId("1");
		Assert.assertEquals(expected, result);
		
		msg = new KVMessage("getreq");
		msg.setKey("balls");
		
		result = handle(msg);
		if (result == null) {
			Assert.fail("Never received response from server");
		}
		expected = new KVMessage("resp");
		expected.setKey("balls");
		expected.setValue("yes");
		Assert.assertEquals(expected, result);
	}
	
	@Test
	public void testAbort() throws Exception {
		KVMessage msg = new KVMessage("putreq");
		msg.setKey("key");
		msg.setValue("value");
		msg.setTpcOpId("5");
		handle(msg);
		msg = new KVMessage("abort");
		msg.setTpcOpId("5");
		handle(msg);
		msg = new KVMessage("getreq");
		msg.setKey("key");
		KVMessage resp = handle(msg);
		KVMessage expected = new KVMessage("resp");
		expected.setMessage("Does not exist");
		Assert.assertEquals(expected, resp);
	}
	
	@Test
	public void testDelete() throws Exception {
		KVMessage msg = new KVMessage("delreq");
		msg.setKey("key");
		msg.setTpcOpId("5");
		KVMessage resp = handle(msg);
		KVMessage expected = new KVMessage("abort");
		expected.setTpcOpId("5");
		expected.setMessage("Does not exist");
		Assert.assertEquals(expected, resp);
		
		// Now put a message
		msg = new KVMessage("putreq");
		msg.setKey("key");
		msg.setValue("value");
		msg.setTpcOpId("5");
		handle(msg);
		msg = new KVMessage("commit");
		msg.setTpcOpId("5");
		handle(msg);
		
		// Message commited. Lets get it.
		msg = new KVMessage("getreq");
		msg.setKey("key");
		resp = handle(msg);
		expected = new KVMessage("resp");
		expected.setKey("key");
		expected.setValue("value");
		Assert.assertEquals(expected, resp);
		
		// Now lets try to phase 1 delete it
		msg = new KVMessage("delreq");
		msg.setKey("key");
		msg.setTpcOpId("10");
		resp = handle(msg);
		expected = new KVMessage("ready");
		expected.setTpcOpId("10");
		Assert.assertEquals(expected, resp);
		
		// Lets try to get it again
		msg = new KVMessage("getreq");
		msg.setKey("key");
		resp = handle(msg);
		expected = new KVMessage("resp");
		expected.setKey("key");
		expected.setValue("value");
		Assert.assertEquals(expected, resp);
		
		// Now lets actually delete it
		msg = new KVMessage("commit");
		msg.setTpcOpId("10");
		resp = handle(msg);
		expected = new KVMessage("ack");
		expected.setTpcOpId("10");
		Assert.assertEquals(expected, resp);
		
		// Lets try to get it again, should fail
		msg = new KVMessage("getreq");
		msg.setKey("key");
		resp = handle(msg);
		expected = new KVMessage("resp");
		expected.setMessage("Does not exist");
		Assert.assertEquals(expected, resp);
	}

	private synchronized KVMessage handle(KVMessage msg) throws IOException, InterruptedException {
		SpySocket sock = new SpySocket();
		sock.setKVMessage(msg);
		handler.handle(sock);
		int count = 1;
		while (sock.getOutputMessage() == null) {
			count++;
			this.wait(100);
			if (count > 50) {
				break;
			}
		}
		return sock.getOutputMessage();
	}
}

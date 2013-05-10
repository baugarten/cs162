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
	public synchronized void testGet() throws Exception {
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

	private KVMessage handle(KVMessage msg) throws IOException, InterruptedException {
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

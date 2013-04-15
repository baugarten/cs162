package test.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVServer;

public class KVServerTest {
	
	private KVServer kvServer;

	@Before
	public void setUp() {
		kvServer =  new KVServer(4, 20);
		
	}
	
	public void testGet() throws Exception {
		assertEquals(null, kvServer.get("not a key"));
	}
	@Test
	public void testPut() throws Exception {
		kvServer.put("key", "value");
		assertEquals("value", kvServer.get("key"));
		assertEquals("value", kvServer.get("key"));
		kvServer.put("key", "notvalue");
		assertEquals("notvalue", kvServer.get("key"));
		assertEquals("notvalue", kvServer.get("key"));
		kvServer.put("anotherkey", "anothervalue");
		assertEquals("anothervalue", kvServer.get("anotherkey"));
	}
	
	@Test
	public void testDel() throws Exception {
		kvServer.put("key", "value");
		kvServer.put("anotherkey", "anothervalue");
		kvServer.put("key2", "anothervalue");
		assertEquals("value", kvServer.get("key"));
		kvServer.del("key");
		assertEquals(null, kvServer.get("key"));
		kvServer.put("key", "what what value");
		assertEquals("what what value", kvServer.get("key"));
		kvServer.del("key");
		assertEquals(null, kvServer.get("key"));
		kvServer.del("anotherkey");
		assertEquals(null, kvServer.get("anotherkey"));
		kvServer.del("key2");
		assertEquals(null, kvServer.get("key2"));
	}
}

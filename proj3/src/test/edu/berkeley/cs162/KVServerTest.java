package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVCache;
import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.KVStore;

public class KVServerTest {
	
	private KVServer kvServer;
	
	private KVCache kvCache;

	private KVStore kvStore;
	
	@Before
	public void setUp() {
		kvServer =  new KVServer(4, 20);
		kvCache = kvServer.getCache();
		kvStore = kvServer.getStore();
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
		try {
			assertEquals(null, kvServer.get("key"));
			fail();
		} catch (KVException e) {
			// Expected
		}
		kvServer.put("key", "what what value");
		assertEquals("what what value", kvServer.get("key"));
		kvServer.del("key");
		try {
			assertEquals(null, kvServer.get("key"));
			fail();
		} catch (KVException e) {
			// Expected
		}
		kvServer.del("anotherkey");
		kvServer.del("key2");
		try {
			kvServer.get("anotherkey");
			fail();
		} catch (KVException e) {
			// Expected
		}
		try {
			kvServer.get("key2");
			fail();
		} catch (KVException e) {
			// Expected
		}
	}
	
	@Test
	public void testCaching() throws Exception {
		String key = "key";
		String value = "value";
		assertNull(kvCache.get(key));
		kvCache.getWriteLock(key).lock();
		kvServer.put(key, value);
		
		assertEquals(value, kvServer.get(key));
		assertEquals(value, kvCache.get(key));
		assertEquals(value, kvStore.get(key));
		
		kvStore.put(key, "NANANANA BATMAN");
		
		assertEquals(value, kvServer.get(key));
		kvCache.getWriteLock(key).unlock();
	}
}

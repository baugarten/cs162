package test.edu.berkeley.cs162;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.*;

public class KVClientTest {
	
	private KVClient client;
	
	@Before
	public void setUp() {
		client = new KVClient("localhost", 8080);
		
	}

	@Test
	public void testPut() throws KVException {
		
		// Put and get 1 kv pair, ensure equality
		client.put("key1", "value1");
		String value = client.get("key1");
		assertEquals("value1", value);
		
		// Overwrite a key and assert its correct value
		client.put("key1", "value2");
		value = client.get("key1");
		assertEquals("value2", value);
		
		// Ensure we can write different key with same value
		client.put("key2", "value2");
		value = client.get("key2");
		assertEquals("value2", value);
	}
	
	@Test
	public void testGet() {
		
		String value;
		// Ensure we throw error for get's of invalid keys
		try {
			value = client.get("Not a key");
		} catch (KVException e) {
			assertEquals(e.getMsg().getMessage(), "Does not exist");
		}
		
	}
	
	@Test
	public void testDel() throws KVException {
		
		String value;
		
		// Put and del a key and ensure its gone
		client.put("key1", "value1");
		client.del("key1");
		
		try {
			value = client.get("key1");
		} catch (KVException e) {
			assertEquals(e.getMsg().getMessage(), "Does not exist");
		}
		
		
	}

}

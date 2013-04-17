package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Test;

public class KVStoreXMLTest {

	public void setUp() {
		
	}
	
	@Test
	public void testDump() throws KVException {
		KVStore store = new KVStore();
		assertEquals(store.toXML(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><KVStore/>");
		store.put("key1", "val1");
		assertEquals(store.toXML(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><KVStore><KVPair><Key>key1</Key><Value>val1</Value></KVPair></KVStore>");
		store.put("key1", "val2");
		assertEquals(store.toXML(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><KVStore><KVPair><Key>key1</Key><Value>val2</Value></KVPair></KVStore>");
		store.put("key2", "val20");
		assertEquals(store.toXML(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><KVStore><KVPair><Key>key1</Key><Value>val2</Value></KVPair><KVPair><Key>key2</Key><Value>val20</Value></KVPair></KVStore>");
		store.del("key1");
		assertEquals(store.toXML(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><KVStore><KVPair><Key>key2</Key><Value>val20</Value></KVPair></KVStore>");
	}

	@Test
	public void testRestore() throws KVException {
		
	}
}

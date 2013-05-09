package test.edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.berkeley.cs162.*;

public class KVStoreXMLTest {

	public void setUp() {
		
	}
	
	@Test
	public void testDumpToFile() throws KVException {
		// basic test to ensure file output works
		KVStore store = new KVStore();
		store.put("a_duck", "goes_quack");
		store.put("key2", "val20");
		store.dumpToFile("KVStore_junit_testDumpToFile.out");
		
		KVStore restore = new KVStore();
		restore.restoreFromFile("KVStore_junit_testDumpToFile.out");
		assertEquals(restore.get("a_duck"), "goes_quack");
		assertEquals(restore.get("key2"), "val20");
	}
	
	@Test
	public void testDump() throws KVException {
		KVStore store = new KVStore();
		// test empty store
		System.out.println(store.toXML());
		assertEquals(store.toXML(), 
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVStore/>\r\n"
				);
		
		// test store with single key
		store.put("key1", "val1");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key1</Key>\r\n"
				+ "<Value>val1</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n",
				store.toXML()
				);
		
		// test with replaced key
		store.put("key1", "val2");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key1</Key>\r\n"
				+ "<Value>val2</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n",
				store.toXML()
				);
		
		// test with two keys
		store.put("key2", "val20");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key1</Key>\r\n"
				+ "<Value>val2</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key2</Key>\r\n"
				+ "<Value>val20</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n",
				store.toXML()
				);
		
		// test with deleted key
		store.del("key1");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key2</Key>\r\n"
				+ "<Value>val20</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n",
				store.toXML()
				);
		
		// test with two keys
		store.put("a_duck", "goes_quack");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>a_duck</Key>\r\n"
				+ "<Value>goes_quack</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key2</Key>\r\n"
				+ "<Value>val20</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n",
				store.toXML()
				);
	}
	/*
	@Test
	public void testRestore() throws KVException {
		KVStore store = new KVStore();
		
		// test empty functionality
		store.put("bad", "badval");
		store.restoreFromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVStore/>");
		try {
			store.get("bad");
			fail("Store should be empty");
		} catch (KVException e) {
			
		}
		
		// test restoring single value
		store.restoreFromString(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key1</Key>\r\n"
				+ "<Value>val1</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n"
				);
		assertEquals(store.get("key1"), "val1");
		
		// test with two values
		store.restoreFromString(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<KVStore>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key1</Key>\r\n"
				+ "<Value>val2</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "<KVPair>\r\n"
				+ "<Key>key2</Key>\r\n"
				+ "<Value>val20</Value>\r\n"
				+ "</KVPair>\r\n"
				+ "</KVStore>\r\n"
				);
		assertEquals(store.get("key1"), "val2");
		assertEquals(store.get("key2"), "val20");
	}
	*/
}

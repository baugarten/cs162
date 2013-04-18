package edu.berkeley.cs162;

import static org.junit.Assert.*;

import org.junit.Test;

public class KVCacheXMLTest {

	public void setUp() {
		
	}
	
	@Test
	public void testToXML() throws Exception {
		KVCache cache = new KVCache(3, 2);
		
		// test empty
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
				+ "<Set id=\"0\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"1\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"2\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "</KVCache>\r\n",
				cache.toXML()
				);
		
		// test single element
		cache.put("duck", "bawk");
		assertEquals(
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
		+ "<Set id=\"0\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
		+ "<Key>duck</Key>\r\n"
		+ "<Value>bawk</Value>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		
		+ "<Set id=\"1\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		
		+ "<Set id=\"2\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		
		+ "</KVCache>\r\n",
		cache.toXML());
		
		// test element replacement
		cache.put("duck", "quack");
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
				+ "<Set id=\"0\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
				+ "<Key>duck</Key>\r\n"
				+ "<Value>quack</Value>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"1\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"2\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "</KVCache>\r\n",
				cache.toXML());
		
		// test isReferenced
		assertEquals("quack", cache.get("duck"));
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
				+ "<Set id=\"0\">\r\n"
				+ "<CacheEntry isReferenced=\"true\" isValid=\"true\">\r\n"
				+ "<Key>duck</Key>\r\n"
				+ "<Value>quack</Value>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"1\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "<Set id=\"2\">\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
				+ "<Key/>\r\n"
				+ "<Value/>\r\n"
				+ "</CacheEntry>\r\n"
				+ "</Set>\r\n"
				
				+ "</KVCache>\r\n",
				cache.toXML());
		
		// test another set and multiples in a set
		cache.put("cow", "moo");
		cache.put("crow", "cawcawcaw");
		assertEquals(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
			+ "<Set id=\"0\">\r\n"
			+ "<CacheEntry isReferenced=\"true\" isValid=\"true\">\r\n"
			+ "<Key>duck</Key>\r\n"
			+ "<Value>quack</Value>\r\n"
			+ "</CacheEntry>\r\n"
			+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
			+ "<Key/>\r\n"
			+ "<Value/>\r\n"
			+ "</CacheEntry>\r\n"
			+ "</Set>\r\n"
			
			+ "<Set id=\"1\">\r\n"
			+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
			+ "<Key/>\r\n"
			+ "<Value/>\r\n"
			+ "</CacheEntry>\r\n"
			+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
			+ "<Key/>\r\n"
			+ "<Value/>\r\n"
			+ "</CacheEntry>\r\n"
			+ "</Set>\r\n"
			
			+ "<Set id=\"2\">\r\n"
			+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
			+ "<Key>cow</Key>\r\n"
			+ "<Value>moo</Value>\r\n"
			+ "</CacheEntry>\r\n"
			+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
			+ "<Key>crow</Key>\r\n"
			+ "<Value>cawcawcaw</Value>\r\n"
			+ "</CacheEntry>\r\n"
			+ "</Set>\r\n"
			+ "</KVCache>\r\n",
			cache.toXML());

		// test deletion
		cache.del("duck");
		assertEquals(
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><KVCache>\r\n"
		+ "<Set id=\"0\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		
		+ "<Set id=\"1\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"false\">\r\n"
		+ "<Key/>\r\n"
		+ "<Value/>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		
		+ "<Set id=\"2\">\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
		+ "<Key>cow</Key>\r\n"
		+ "<Value>moo</Value>\r\n"
		+ "</CacheEntry>\r\n"
		+ "<CacheEntry isReferenced=\"false\" isValid=\"true\">\r\n"
		+ "<Key>crow</Key>\r\n"
		+ "<Value>cawcawcaw</Value>\r\n"
		+ "</CacheEntry>\r\n"
		+ "</Set>\r\n"
		+ "</KVCache>\r\n",
		cache.toXML());
	}

}

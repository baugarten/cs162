package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.junit.Test;

import edu.berkeley.cs162.KVCache.Entry;
import edu.berkeley.cs162.KVCache.Set;

public class KVCacheTest {


	@Test
	//set size =1, cache size =1
	public void testCacheFull() {
		int numberSets=1;
		int cacheSize=1;
		String key="1";
		String value="1st item";
		KVCache KVCache= new KVCache(numberSets,cacheSize);
		KVCache.put(key,value);
		List<Set> listSets= KVCache.getListSet();
		Set s= listSets.get(key.hashCode()%numberSets);
		Map<String,Entry> cacheMap=s.getMap();
		assertEquals(value, cacheMap.get(key).value);
		key="2";
		value="2nd item";
		KVCache.put(key, value);
		assertEquals(value, cacheMap.get(key).value);
	}

	@Test
	//set size =1, cache size =3
	public void testPut() {
		int numberSets=1;
		int cacheSize=3;
		String []key={"1","2","3"};
		String []value={"1st item","2nd item","3rd item"};
		KVCache KVCache= new KVCache(numberSets,cacheSize);
		for(int i =0; i<key.length; i++){
			KVCache.put(key[i],value[i]);
		}
		List<Set> listSets= KVCache.getListSet();

		for(int i =0; i<key.length; i++){
			Set s= listSets.get(key[i].hashCode()%numberSets);
			Map<String,Entry> cacheMap=s.getMap();

			assertEquals(value[i], cacheMap.get(key[i]).value);
		}
		
		
		Set s= listSets.get(key[0].hashCode()%numberSets);
		Map<String,Entry> cacheMap=s.getMap();
		//2nd time chance
		//evict key[2]
		String newKey="new key";
		String newValue="new value";
		KVCache.get(key[0]);
		KVCache.get(key[1]);
		
		assertEquals(0,s.getKeysPtr());	//clock hand beginning
		
		KVCache.put(newKey, newValue);
		List<String> listHashKeys=s.getHashKeys();
		
		assertEquals(cacheSize,listHashKeys.size());
		
		//assert the listHashKeys are correct
		assertEquals(listHashKeys.get(0),key[0]);
		assertEquals(listHashKeys.get(1),key[1]);
		assertEquals(listHashKeys.get(2),newKey);
		
		//assert the cacheMap are correct
		
		assertEquals(null,KVCache.get(key[2]));
		assertEquals(newValue,cacheMap.get(newKey).value);
		assertEquals(value[0],cacheMap.get(key[0]).value);
		assertEquals(value[1],cacheMap.get(key[1]).value);
		
		
		assertEquals(0,s.getKeysPtr());	//clock hand updated
		
		
		//evict key[1]

		KVCache.get(key[0]);
		String newKey2="new key2";
		String newValue2="new value2";
		KVCache.put(newKey2, newValue2);
		assertEquals(null,KVCache.get(key[1]));
		
		//assert the listHashKeys are correct
		assertEquals(listHashKeys.get(0),key[0]);
		assertEquals(listHashKeys.get(1),newKey2);
		assertEquals(listHashKeys.get(2),newKey);
		
		//assert the cacheMap are correct
		
		assertEquals(null,KVCache.get(key[2]));
		assertEquals(newValue,cacheMap.get(newKey).value);
		assertEquals(value[0],cacheMap.get(key[0]).value);
		assertEquals(newValue2,cacheMap.get(newKey2).value);
		
	}

	@Test
	//set size =1, cache size =1
	public void testGet() {
		int numberSets=1;
		int cacheSize=3;
		String []key={"1","2","3"};
		String []value={"1st item","2nd item","3rd item"};
		KVCache KVCache= new KVCache(numberSets,cacheSize);
		
		for(int i =0; i<key.length; i++){
			KVCache.put(key[i],value[i]);
		}
		List<Set> listSets= KVCache.getListSet();
		
		
		Set s= listSets.get(key[1].hashCode()%numberSets);
		Map<String,Entry> cacheMap=s.getMap();
		
		assertFalse(cacheMap.get(key[1]).used);
		KVCache.get(key[1]);
		assertTrue(cacheMap.get(key[1]).used);
		KVCache.get(key[1]);
		assertTrue(cacheMap.get(key[1]).used);
		
		
		//no key exists, return null
		assertEquals(null, cacheMap.get("invalid key"));

	}

	@Test
	//set size =1, cache size =1
	public void testDel() {
		int numberSets=1;
		int cacheSize=3;
		String []key={"1","2","3"};
		String []value={"1st item","2nd item","3rd item"};
		KVCache KVCache= new KVCache(numberSets,cacheSize);
		
		Set s= 	KVCache.getListSet().get(key[1].hashCode()%numberSets);
		List<String> listHashKeys=s.getHashKeys();
		for(int i =0; i<key.length; i++){
			KVCache.put(key[i],value[i]);
		}
		
		//del key[1]
		KVCache.del(key[1]);
		//assert the listHashKeys are correct
		assertEquals(listHashKeys.get(0),key[0]);
		assertEquals(listHashKeys.get(1),null);
		assertEquals(listHashKeys.get(2),key[2]);
		
		//assert the cacheMap are correct
		Map<String,Entry> cacheMap=s.getMap();
		assertEquals(value[0],KVCache.get(key[0]));
		assertEquals(null,KVCache.get(key[1]));
		assertEquals(value[2],cacheMap.get(key[2]).value);

	}
	
	@Test
	public void testGetWriteLock() {
		int numberSets=10;
		int cacheSize=3;

		KVCache KVCache= new KVCache(numberSets,cacheSize);
		String []key={"1","2","3"};
		String []value={"1st item","2nd item","3rd item"};
		for(int i =0; i<key.length; i++){
			KVCache.put(key[i],value[i]);
		}

		List<Set> listSets= KVCache.getListSet();
		for(int i =0; i<key.length; i++){
			Set s= listSets.get(key[i].hashCode()%numberSets);
			WriteLock lock = s.getWriteLock();

			assertEquals(KVCache.getWriteLock(key[i]),lock);
		}
	}


}

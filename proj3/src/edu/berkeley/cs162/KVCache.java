/**
 * Implementation of a set-associative cache.
 * 
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 * 
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *    
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs162;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on the eviction policy.
 */
public class KVCache implements KeyValueInterface {     
	private int numSets = 100;
        private int maxElemsPerSet = 10;
        
        private List<Set> listSet;
        
        public List<Set> getListSet(){
        	return listSet;
        }
        
        class Set {
        	private Map<String, Entry> map;
        	private WriteLock writeLock;
        	private List<String> hashKeys;	//record the position of each hashkey
        	private int keysPtr;		//clock hand
        	
        	public Map<String,Entry> getMap(){
        		return map;
        	}
        	
        	public WriteLock getWriteLock(){
        		return writeLock;
        	}
        	
        	public List<String> getHashKeys(){
        		return hashKeys;
        	}
        	
        	public int getKeysPtr(){
        		return keysPtr;
        	}
        	
        	
        	Set(Map<String, Entry> map){
        		this.map=map;
        		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        		writeLock =lock.writeLock();
        		this.hashKeys= new LinkedList<String>();
        		for(int i =0; i<maxElemsPerSet; i++){
        			hashKeys.add("");
        			hashKeys.set(i, null);
        		}

        		this.keysPtr=0;
        	
        	}
        }

        class Entry {
        	boolean used;
        	String value;
        	
        	Entry(String value){
        		this.used=false;
        		this.value=value;
        	}
        }
                
        /**
         * Creates a new LRU cache.
         * @param cacheSize     the maximum number of entries that will be kept in this cache.
         */
        public KVCache(int numSets, int maxElemsPerSet) {
                this.numSets = numSets;
                this.maxElemsPerSet = maxElemsPerSet;     
                // TODO: Implement Me!
                this.listSet=new ArrayList<Set>();
                for (int i=0; i<numSets; i++){
                	listSet.add(new Set(new HashMap<String, Entry>()));
                }

        }

        /**
         * Retrieves an entry from the cache.
         * Assumes the corresponding set has already been locked for writing.
         * @param key the key whose associated value is to be returned.
         * @return the value associated to this key, or null if no value with this key exists in the cache.
         */
        public String get(String key) {
                // Must be called before anything else
                AutoGrader.agCacheGetStarted(key);
                AutoGrader.agCacheGetDelay();
        
                // TODO: Implement Me!
            	int setId=getSetId(key);
            	Set set = listSet.get(setId);
            	Map<String, Entry> M = set.map;
            	if (M.containsKey(key)){
            		Entry entry= M.get(key);
            		entry.used=true;
            		AutoGrader.agCacheGetFinished(key);
            		return entry.value;
            	}
            	
                // Must be called before returning
                AutoGrader.agCacheGetFinished(key);
                return null;
        }

        /**
         * Adds an entry to this cache.
         * If an entry with the specified key already exists in the cache, it is replaced by the new entry.
         * If the cache is full, an entry is removed from the cache based on the eviction policy
         * Assumes the corresponding set has already been locked for writing.
         * @param key   the key with which the specified value is to be associated.
         * @param value a value to be associated with the specified key.
         * @return true is something has been overwritten 
         */
        public void put(String key, String value) {
                // Must be called before anything else
                AutoGrader.agCachePutStarted(key, value);
                AutoGrader.agCachePutDelay();

            	int setId=getSetId(key);
            	Set set = listSet.get(setId);

            	Map<String, Entry> M = set.map;
            	List<String> listKeys =set.hashKeys;
            	int ptr=set.keysPtr;
            
            	if (M.size() < maxElemsPerSet || M.containsKey(key)){
            		if (!M.containsKey(key)){
            			int temp=listKeys.indexOf(null);
            			listKeys.set(temp,key);
            		}
            				
            	}
            			
            	//eviction
            	//(M.size() >= maxElemsPerSet)
            	else{
            		while(true){		
            			String tempKey = listKeys.get(ptr);
            			Entry entry = M.get(tempKey);
            			if(entry.used == false){
            				M.remove(tempKey);
            				int tempIndex= listKeys.indexOf(tempKey);
            				listKeys.set(tempIndex, key);
            				//tempIndex++;
            				set.keysPtr =++tempIndex%this.maxElemsPerSet;
            				break;
            			}
            			else{
	            			entry.used=false;
            			}
            						
            			if(ptr >= maxElemsPerSet-1){
            				ptr=0;
            			}
            			else{
            				ptr++;
            			}
	            					
            		}			
            	}
                M.put(key, new Entry(value));
        		
                // Must be called before returning
                AutoGrader.agCachePutFinished(key, value);
        }

        /**
         * Removes an entry from this cache.
         * Assumes the corresponding set has already been locked for writing.
         * @param key   the key with which the specified value is to be associated.
         */
        public void del (String key) {
                // Must be called before anything else
                AutoGrader.agCacheGetStarted(key);
                AutoGrader.agCacheDelDelay();
                
                // TODO: Implement Me!
                int setId=getSetId(key);
               	Set set = listSet.get(setId);
                Map<String, Entry> M = set.map;
                List<String> listKeys = set.hashKeys;
                if(M.containsKey(key)){
                	M.remove(key);
                	int index=listKeys.indexOf(key);
                	listKeys.set(index,null);
                }

                // Must be called before returning
                AutoGrader.agCacheDelFinished(key);
        }
        
        /**
         * @param key
         * @return      the write lock of the set that contains key.
         */
        public WriteLock getWriteLock(String key) {
        	int setId=getSetId(key);
        	Set set = listSet.get(setId);
        	return set.writeLock;
        }
        
        /**
         * 
         * @param key
         * @return      set of the key
         */
        private int getSetId(String key) {
               	return Math.abs(key.hashCode()) % numSets;
        }
        
	public String toXML() {
    	try {
    		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.newDocument();
    		doc.setXmlStandalone(true);
    		
    		// build DOM
    		Element kvCacheNode = doc.createElement("KVCache");
    		doc.appendChild(kvCacheNode);
    		
    		for (int setInd=0; setInd<numSets; setInd++) {
    			Element setNode = doc.createElement("Set");
    			setNode.setAttribute("id", Integer.toString(setInd));
    			kvCacheNode.appendChild(setNode);
    		
    			Set set = listSet.get(setInd);

    			for (String hashKey : set.hashKeys) {
    				Entry entry = set.map.get(hashKey);
    				Element cacheEntryNode = doc.createElement("CacheEntry");
        			Element keyNode = doc.createElement("Key");
        			Element valueNode = doc.createElement("Value");

    				if (hashKey != null && entry != null) {
        				cacheEntryNode.setAttribute("isReferenced", Boolean.toString(entry.used));
        				cacheEntryNode.setAttribute("isValid", Boolean.toString(true));
            			keyNode.setTextContent(hashKey);
            			valueNode.setTextContent(entry.value);
    				} else {
        				cacheEntryNode.setAttribute("isReferenced", Boolean.toString(false));
        				cacheEntryNode.setAttribute("isValid", Boolean.toString(false));
            			keyNode.setTextContent("");
            			valueNode.setTextContent("");
    				}

    				setNode.appendChild(cacheEntryNode);
    				
        			cacheEntryNode.appendChild(keyNode);
        			cacheEntryNode.appendChild(valueNode);
    			}
    		}
    		
    		// output to string
    		TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    		StringWriter writer = new StringWriter();
    		transformer.transform(new DOMSource(doc), new StreamResult(writer));
    		return writer.getBuffer().toString();
    	} catch (Exception e) {
    		System.err.println("KVCache::toXML: Exception building DOM: " + e);
    	}
		return "";
	}
    
}

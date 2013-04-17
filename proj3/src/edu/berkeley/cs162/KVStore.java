/**
 * Persistent Key-Value storage layer. Current implementation is transient, 
 * but assume to be backed on disk when you do your project.
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
import java.util.*;
import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
 

/**
 * This is a dummy KeyValue Store. Ideally this would go to disk, 
 * or some other backing store. For this project, we simulate the disk like 
 * system using a manual delay.
 *
 */
public class KVStore implements KeyValueInterface {
	private Dictionary<String, String> store 	= null;
	
	public KVStore() {
		resetStore();
	}

	private void resetStore() {
		store = new Hashtable<String, String>();
	}
	
	public boolean put(String key, String value) throws KVException {
		AutoGrader.agStorePutStarted(key, value);
		
		try {
			putDelay();
			store.put(key, value);
			return false;
		} finally {
			AutoGrader.agStorePutFinished(key, value);
		}
	}
	
	public String get(String key) throws KVException {
		AutoGrader.agStoreGetStarted(key);
		
		try {
			getDelay();
			String retVal = this.store.get(key);
			if (retVal == null) {
			    KVMessage msg = new KVMessage("resp", "key \"" + key + "\" does not exist in store");
			    throw new KVException(msg);
			}
			return retVal;
		} finally {
			AutoGrader.agStoreGetFinished(key);
		}
	}
	
	public void del(String key) throws KVException {
		AutoGrader.agStoreDelStarted(key);

		try {
			delDelay();
			if(key != null)
				this.store.remove(key);
		} finally {
			AutoGrader.agStoreDelFinished(key);
		}
	}
	
	private void getDelay() {
		AutoGrader.agStoreDelay();
	}
	
	private void putDelay() {
		AutoGrader.agStoreDelay();
	}
	
	private void delDelay() {
		AutoGrader.agStoreDelay();
	}
	
    public String toXML() throws KVException {
    	try {
    		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.newDocument();
    		
    		// build DOM
    		Element kvStoreNode = doc.createElement("KVStore");
    		doc.appendChild(kvStoreNode);
    		
    		List<String> keyList = Collections.list(store.keys());
    		java.util.Collections.sort(keyList);
    		
    		for (String key : keyList) {
//    			String key = keys.nextElement();
    			String value = store.get(key);
    			
    			Element kvPairNode = doc.createElement("KVPair");
    			kvStoreNode.appendChild(kvPairNode);
    			
    			Element keyNode = doc.createElement("Key");
    			keyNode.setTextContent(key);
    			kvPairNode.appendChild(keyNode);
    			
    			Element valueNode = doc.createElement("Value");
    			valueNode.setTextContent(value);
    			kvPairNode.appendChild(valueNode);
    		}
    		
    		// output to string
    		TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    		StringWriter writer = new StringWriter();
    		transformer.transform(new DOMSource(doc), new StreamResult(writer));
    		return writer.getBuffer().toString();
    	} catch (Exception e) {
    		System.err.println("KVStore::dumpToString: Exception building DOM: " + e);
    	}
		return "";
    }        

    public void dumpToFile(String fileName) throws KVException {
        String xmlContent = toXML();
        
        try {
	    	FileWriter fw = new FileWriter(fileName);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(xmlContent);
			bw.close();
        } catch (IOException e) {
        	System.err.println("KVStore::dumpToFile: file output exception: " + e);
        }
    }
    
    public void restoreFromFile(String fileName) throws KVException{
    	BufferedReader br = null;
        try {
        	StringBuilder sb = new StringBuilder();
        	br = new BufferedReader(new FileReader(fileName));
        	String line = br.readLine();
        	
        	while (line != null) {
        		sb.append(line);
        		sb.append("\n");
        		
        		line = br.readLine();
        	}
        	
        	restoreFromString(sb.toString());
        } catch (IOException e) {
        	System.err.println("KVStore::dumpToString: exception reading file: " + e);
        } finally {
        	if (br != null) {
        		try {
        			br.close();
	        	} catch (IOException e) {        		
	        	}
        	}
        }
    }
    
    public void restoreFromString(String xmlContent) throws KVException {
    	try {
	    	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	
	        Document doc = docBuilder.parse(new InputSource(new StringReader(xmlContent)));
	        
	        resetStore();
	        
	        Node root = doc.getFirstChild();
	        if (root.getNodeName() != "KVStore") {
	        	System.err.println("KVStore::restoreFromString: input DOM structure error.");
	        	return;
	        }
	        
	        Node kvPairNode = root.getFirstChild();
	        while (kvPairNode != null) {
	        	String nodeKey = null, nodeValue = null;

	        	if (kvPairNode.getNodeName() != "KVPair") {
	        		System.err.println("KVStore::restoreFromString: input DOM structure error.");
		        	return;	        		
	        	}
	        	
	        	Node kvAttrNode = kvPairNode.getFirstChild();
	        	while (kvAttrNode != null) {
	        		String attrType = kvAttrNode.getNodeName();
	        		if (attrType == "Key") {
	        			if (nodeKey != null) {
	        				System.err.println("KVStore::restoreFromString: input DOM structure error.");
				        	return;	        				
	        			} else {
	        				nodeKey = kvAttrNode.getTextContent();
	        			}
	        		} else if (attrType == "Value") {
	        			if (nodeValue != null) {
	        				System.err.println("KVStore::restoreFromString: input DOM structure error.");
				        	return;	        				
	        			} else {
	        				nodeValue = kvAttrNode.getTextContent();
	        			}
	        		} else {
	        			System.err.println("KVStore::restoreFromString: input DOM structure error.");
			        	return;	        			
	        		}

	        		kvAttrNode = kvAttrNode.getNextSibling();
	        	}
	        	
        		if (nodeKey == null || nodeValue == null) {
        			System.err.println("KVStore::restoreFromString: input DOM structure error.");
		        	return;
        		}
        		
        		store.put(nodeKey, nodeValue);
	        	
	        	kvPairNode = kvPairNode.getNextSibling();
	        }
	        
	        
    	} catch (Exception e) {
    		System.err.println("KVStore::restoreFromString: Exception building DOM: " + e);
    	}
	}
}

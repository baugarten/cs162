/**
 * XML Parsing library for the key-value store
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;


/**
 * This is the object that is used to generate messages the XML based messages 
 * for communication between clients and servers. 
 */
public class KVMessage {
	private String msgType = null;
	private String key = null;
	private String value = null;
	private String message = null;
	
	
	private static void fillSet(Set<String> set){
		set.add("getreq");
		set.add("putreq");
		set.add("delreq");
		set.add("resp");
	}
	
	private static final Set<String> typeSet;
	static {
		final Set<String> temp = new HashSet<String>();
		fillSet(temp);
		typeSet = Collections.unmodifiableSet(temp);
	}
	
	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final String getValue() {
		return value;
	}

	public final void setValue(String value) {
		this.value = value;
	}

	public final String getMessage() {
		return message;
	}

	public final void setMessage(String message) {
		this.message = message;
	}

	public String getMsgType() {
		return msgType;
	}

	/* Solution from http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html */
	private class NoCloseInputStream extends FilterInputStream {
	    public NoCloseInputStream(InputStream in) {
	        super(in);
	    }
	    
	    public void close() {} // ignore close
	}
	
	/***
	 * 
	 * @param msgType
	 * @throws KVException of type "resp" with message "Message format incorrect" if msgType is unknown
	 */
	public KVMessage(String msgType) throws KVException {
		
		if(!typeSet.contains(msgType)){
			throw new KVException(new KVMessage("resp","Message format incorrect"));
		}
	    
		this.msgType = msgType;
	}
	
	public KVMessage(String msgType, String message) throws KVException {
		
		if(!typeSet.contains(msgType)){
			throw new KVException(new KVMessage("resp","Message format incorrect"));
		}
	    
		this.msgType = msgType;
		this.message = message;
	}
	
	 /***
     * Parse KVMessage from incoming network connection
     * @param sock
     * @throws KVException if there is an error in parsing the message. The exception should be of type "resp" and message should be :
     * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
     * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
     * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
     */
	public KVMessage(InputStream input) throws KVException {
		NoCloseInputStream noCloseStream;
		ObjectInputStream in = null;
		String attr, temp;
		Node root, keyNode, valueNode, msgNode;
		NodeList children, keyEle, valueEle, msgEle; 
		try{
			//create a DOM from the InputStream input
			noCloseStream = new NoCloseInputStream(input);
			in = new ObjectInputStream(noCloseStream);
			temp = (String) in.readObject();
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			InputSource source = new InputSource(new StringReader(temp));
			Document document = factory.newDocumentBuilder().parse(source);
			document.getDocumentElement().normalize();
			
			//Parse the DOM and set appropriate variables
			//The rootElement is KVMessage
			NodeList nList = document.getElementsByTagName("KVMessage");
			if (nList.getLength() != 1){
				throw new KVException(new KVMessage("resp","Message format incorrect"));
			}			
			root = nList.item(0);
			
			//Get the type of the message
			NamedNodeMap attributes = root.getAttributes();
			if (attributes.getLength() != 1){
				throw new KVException(new KVMessage("resp","Message format incorrect"));
			}
			if (!attributes.item(0).getNodeName().equals("type")){
				throw new KVException(new KVMessage("resp","Message format incorrect"));
			}
			attr = attributes.item(0).getNodeValue();
			if(!typeSet.contains(attr)){
				throw new KVException(new KVMessage("resp","Message format incorrect"));
			}
			this.msgType = attr;
			
			//Get the key, value, and message	
			if(attr.equals("getreq") || attr.equals("delreq") || attr.equals("putreq")){
				children = document.getElementsByTagName("Key");
				if(children.getLength() != 1){
					throw new KVException(new KVMessage("resp","Message format incorrect"));
				}
				keyNode = children.item(0);
				this.key = keyNode.getTextContent();
				
				if(key.length() == 0){
					throw new KVException(new KVMessage("resp", "Undersized key"));
				} 
				
				if(attr.equals("putreq")){
					if(this.key.length() > 256){
						throw new KVException(new KVMessage("resp","Oversized key"));
					}
					
					children = document.getElementsByTagName("Value");
					if(children.getLength() != 1){
						throw new KVException(new KVMessage("resp","Message format incorrect"));
					}
					valueNode = children.item(0);
					this.value = valueNode.getTextContent();
					if(this.value.length() > 256000){
						throw new KVException(new KVMessage("resp","Oversized value"));
					}
					
					if(this.value.length() == 0){
						throw new KVException(new KVMessage("resp", "Undersized value"));
					}
				}
			}
			else {
				keyEle = document.getElementsByTagName("Key");
				valueEle = document.getElementsByTagName("Value");
				msgEle = document.getElementsByTagName("Message");
				
				if(keyEle.getLength() == 1 && valueEle.getLength() == 1 && msgEle.getLength() == 0){
					keyNode = keyEle.item(0);
					this.key = keyNode.getTextContent();
					if(this.key.length() > 256 || this.key.length() == 0){
						throw new KVException(new KVMessage("resp","Oversized key"));
					}
					
					valueNode = valueEle.item(0);
					this.value = valueNode.getTextContent();
					if(this.value.length() > 256000 || this.key.length() == 0){
						throw new KVException(new KVMessage("resp","Oversized value"));
					}
				}
				else if (keyEle.getLength() == 0 && valueEle.getLength() == 0 && msgEle.getLength() == 1){
					msgNode = msgEle.item(0);
					this.message = msgNode.getTextContent();
				}
				else {
					throw new KVException(new KVMessage("resp","Message format incorrect"));
				}
			}	
		} catch (SAXException e){
			throw new KVException(new KVMessage("resp", "XML Error: Received unparseable message"));
		} catch (IOException e){
			throw new KVException(new KVMessage("resp", "Network Error: Could not receive data"));
		} catch (ParserConfigurationException e) {
			throw new KVException(new KVMessage("resp", "Unknown Error: Something is wrong"));
		} catch (ClassNotFoundException e){
			throw new KVException(new KVMessage("resp","Unknown Error: Something is wrong"));
		}
		finally {
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * Generate the XML representation for this message.
	 * @return the XML String
	 * @throws KVException if not enough data is available to generate a valid KV XML message
	 */
	public String toXML() throws KVException {

		try{
			Element rootEle, keyEle, valueEle, msgEle;
			
			DocumentBuilderFactory docFac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFac.newDocumentBuilder();
			
			//root element
			Document doc = docBuilder.newDocument();
			doc.setXmlStandalone(true);
			rootEle = doc.createElement("KVMessage");
			doc.appendChild(rootEle);
			
			//set attribute to KVMessage node
			rootEle.setAttribute("type",msgType);
			
			//add nodes to the DOM
			if(msgType.equals("getreq") || msgType.equals("putreq") || msgType.equals("delreq")){
				if(key == null){
					throw new KVException(
							new KVMessage("resp", "Unknown Error: Not enough data available to generate a valid XML message"));
				}
				if(key.length() == 0){
					throw new KVException(new KVMessage("resp", "Undersized key"));
				}
				keyEle = doc.createElement("Key");
				keyEle.appendChild(doc.createTextNode(key));
				rootEle.appendChild(keyEle);
				
				if(msgType.equals("putreq")){
					if(value == null){
						throw new KVException(
								new KVMessage("resp", "Unknown Error: Not enough data available to generate a valid XML message"));
					}
					
					if(key.length() > 256){
						throw new KVException(new KVMessage("resp","Oversized key"));
					}

					if(value.length() > 256000){
						throw new KVException(new KVMessage("resp","Oversized value"));
					}
					
					if(value.length() == 0){
						throw new KVException(new KVMessage("resp", "Undersized value"));
					}
					
					valueEle = doc.createElement("Value");
					valueEle.appendChild(doc.createTextNode(value));
					rootEle.appendChild(valueEle);
				}
			} else {
				if(key != null && value != null && message == null){
					
					keyEle = doc.createElement("Key");
					keyEle.appendChild(doc.createTextNode(key));
					rootEle.appendChild(keyEle);

					valueEle = doc.createElement("Value");
					valueEle.appendChild(doc.createTextNode(value));
					rootEle.appendChild(valueEle);
					
				}
				else if(message != null && key == null && value == null){
					msgEle = doc.createElement("Message");
					msgEle.appendChild(doc.createTextNode(message));
					rootEle.appendChild(msgEle);
				}
				else {
					throw new KVException(
						new KVMessage("resp", "Unknown Error: Not enough data available to generate a valid XML message"));
				}
			}
			
			//convert the DOM to a string and return that string
			StringWriter stringWriter = new StringWriter();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
			String xml = stringWriter.toString();
			return xml;
			
		} catch (ParserConfigurationException pce){
			pce.printStackTrace();
			return null;
		} catch (TransformerException tfe){
			tfe.printStackTrace();
			return null;
		}
	}
	
	public void sendMessage(Socket sock) throws KVException {
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(sock.getOutputStream());
			String temp = toXML();
			out.writeObject(temp);
			out.flush();
			//out.close();
		} catch (KVException e){
			if(out != null){
				try {
					out.close();
				} catch (IOException e1) {
				}
			}
			throw e;
		}
		catch (Exception e){
			if(out != null){
				try{
					out.close();
				} catch (IOException e2){
				}
			}
			throw new KVException(new KVMessage("resp","Network Error: Could not send data"));
		} 
	}
}

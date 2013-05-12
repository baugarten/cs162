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

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * This is the object that is used to generate messages the XML based messages 
 * for communication between clients and servers. 
 */
public class KVMessage implements Serializable {
	
	private static final long serialVersionUID = 6473128480951955693L;
	
	private String msgType = null;
	private String key = null;
	private String value = null;
	private String message = null;
    private String tpcOpId = null; 
    
	private static void fillSet(Set<String> set){
		set.add("getreq");
		set.add("putreq");
		set.add("delreq");
		set.add("resp");
		set.add("ready");
		set.add("commit");
		set.add("abort");
		set.add("ack");
		set.add("ignoreNext");
		set.add("register");
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
	
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getTpcOpId() {
		return tpcOpId;
	}

	public void setTpcOpId(String tpcOpId) {
		this.tpcOpId = tpcOpId;
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
	
	public KVMessage(String msgType, String key, String value) throws KVException {
		if(!typeSet.contains(msgType)){
			throw new KVException(new KVMessage("resp","Message format incorrect"));
		}
	    
		this.msgType = msgType;
		this.key = key;
		this.value = value;
	}
	
	 /***
     * Parse KVMessage from incoming network connection
     * @param sock
     * @throws KVException if there is an error in parsing the message. The exception should be of type "resp and message should be :
     * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
     * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
     * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
     */
	public KVMessage(InputStream input) throws KVException {
   		NoCloseInputStream noCloseStream;
   		ObjectInputStream in = null;
   		String attr, temp;
   		Node root, keyNode, valueNode, msgNode, tpcIdNode;
   		NodeList children, keyEle, valueEle, msgEle; 
   		try{
   			//create a DOM from the InputStream input
   			noCloseStream = new NoCloseInputStream(input);
   			StringBuffer tmp = new StringBuffer();
   			int t;
   			while (!(tmp.toString().endsWith("</KVMessage>") || tmp.toString().matches(".*<KVMessage[^<>]+/>$")) && (t = noCloseStream.read()) > 0) { 
   				tmp.append((char) t);
   			}
   			temp = tmp.toString();
   			
   			DocumentBuilderFactory factory = DocumentBuilderFactory
   					.newInstance();
   			InputSource source = new InputSource(new StringReader(temp.toString()));
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
   					/*
   					if(this.key.length() > 256){
   						throw new KVException(new KVMessage("resp","Oversized key"));
   					}
   					*/
   					
   					children = document.getElementsByTagName("Value");
   					if(children.getLength() != 1){
   						throw new KVException(new KVMessage("resp","Message format incorrect"));
   					}
   					valueNode = children.item(0);
   					this.value = valueNode.getTextContent();
   					/*
   					if(this.value.length() > 256000){
   						throw new KVException(new KVMessage("resp","Oversized value"));
   					}
   					
   					if(this.value.length() == 0){
   						throw new KVException(new KVMessage("resp", "Undersized value"));
   					}
   					*/
   					children = document.getElementsByTagName("TPCOpId");
   					if(children.getLength() > 1){
   						throw new KVException(new KVMessage("resp", "Message format incorrect"));
   					}
   					if(children.getLength() == 1){
   						tpcIdNode = children.item(0);
   						this.tpcOpId = tpcIdNode.getTextContent();
   					}
   				}
   				
   				if(attr.equals("delreq")){
   					children = document.getElementsByTagName("TPCOpId");
   					if(children.getLength() > 1){
   						throw new KVException(new KVMessage("resp", "Message format incorrect"));
   					}
   					if(children.getLength() == 1){
   						tpcIdNode = children.item(0);
   						this.tpcOpId = tpcIdNode.getTextContent();
   					}
   				}
   			}
   			else if (attr.equals("ready") || attr.equals("commit") || attr.equals("ack")){
				children = document.getElementsByTagName("TPCOpId");
				if (children.getLength() != 1) {
					throw new KVException(new KVMessage("resp",
							"Message format incorrect"));
				}
				if (children.getLength() == 1) {
					tpcIdNode = children.item(0);
					this.tpcOpId = tpcIdNode.getTextContent();
				}
   			}
   			else if (attr.equals("abort")){
   				children = document.getElementsByTagName("TPCOpId");
				if (children.getLength() != 1) {
					throw new KVException(new KVMessage("resp",
							"Message format incorrect"));
				}
				tpcIdNode = children.item(0);
				this.tpcOpId = tpcIdNode.getTextContent();
				
				children = document.getElementsByTagName("Message");
				if (children.getLength() > 1) {
					throw new KVException(new KVMessage("resp",
							"Message format incorrect"));
				}
				if (children.getLength() == 1) {
					msgNode = children.item(0);
					this.message = msgNode.getTextContent();
				}
   			}
   			else if (attr.equals("register")){
   				children = document.getElementsByTagName("Message");
				if (children.getLength() != 1) {
					throw new KVException(new KVMessage("resp",
							"Message format incorrect"));
				}
				
				msgNode = children.item(0);
				this.message = msgNode.getTextContent();		
   			}
   			else if (attr.equals("ignoreNext")){
	   				children = document.getElementsByTagName("Message");
					if (children.getLength() != 0) {
						throw new KVException(new KVMessage("resp",
								"Message format incorrect"));
					}	
   			}
   			else {
   				keyEle = document.getElementsByTagName("Key");
   				valueEle = document.getElementsByTagName("Value");
   				msgEle = document.getElementsByTagName("Message");
   				
   				if(keyEle.getLength() == 1 && valueEle.getLength() == 1 && msgEle.getLength() == 0){
   					keyNode = keyEle.item(0);
   					this.key = keyNode.getTextContent();
   					/*
   					if(this.key.length() > 256 || this.key.length() == 0){
   						throw new KVException(new KVMessage("resp","Oversized key"));
   					}
   					*/
   					valueNode = valueEle.item(0);
   					this.value = valueNode.getTextContent();
   					/*
   					if(this.value.length() > 256000 || this.key.length() == 0){
   						throw new KVException(new KVMessage("resp","Oversized value"));
   					}
   					*/
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
   			e.printStackTrace();
   			throw new KVException(new KVMessage("resp", "Network Error: Could not receive data"));
   		} catch (ParserConfigurationException e) {
   			throw new KVException(new KVMessage("resp", "Unknown Error: Something is wrong"));
		} finally {
   			if(in != null){
   				try {
   					in.close();
   				} catch (IOException e) {
   				}
   			}
   		}
	}
	
	/**
	 * 
	 * @param sock Socket to receive from
	 * @throws KVException if there is an error in parsing the message. The exception should be of type "resp and message should be :
	 * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
	 * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
	 * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
	 */
	public KVMessage(Socket sock) throws KVException {
		this(KVgetInputStream(sock));
	}

	private static InputStream KVgetInputStream(Socket sock) throws KVException {
		try {
			return sock.getInputStream();
		} catch (IOException e) {
			throw new KVException(new KVMessage("resp",
					"Unknown Error: Could not get InputStream"));
		}
	}

	/**
	 * 
	 * @param sock Socket to receive from
	 * @param timeout Give up after timeout milliseconds
	 * @throws KVException if there is an error in parsing the message. The exception should be of type "resp and message should be :
	 * a. "XML Error: Received unparseable message" - if the received message is not valid XML.
	 * b. "Network Error: Could not receive data" - if there is a network error causing an incomplete parsing of the message.
	 * c. "Message format incorrect" - if there message does not conform to the required specifications. Examples include incorrect message type. 
	 */
	public KVMessage(Socket sock, int timeout) throws KVException {
		this(KVgetInputStreamAndTO(sock, timeout));
	}

	private static InputStream KVgetInputStreamAndTO(Socket sock, int timeout)
			throws KVException {
		try {
			sock.setSoTimeout(timeout);
			return sock.getInputStream();
		} catch (SocketException e) {
			throw new KVException(new KVMessage("resp",
					"Unknown Error: Could not set socket timeout"));
		} catch (IOException e) {
			try {
				sock.setSoTimeout(0);
			} catch (SocketException e1) {
				throw new KVException(new KVMessage("resp",
						"Unknown Error: Could not set socket timeout"));
			}
			throw new KVException(new KVMessage("resp",
					"Unknown Error: Could not get InputStream"));
		}
    }
	
	/**
	 * Copy constructor
	 * 
	 * @param kvm
	 */
	public KVMessage(KVMessage kvm) {
		this.msgType = kvm.msgType;
		this.key = kvm.key;
		this.value = kvm.value;
		this.message = kvm.message;
		this.tpcOpId = kvm.tpcOpId;
	}

	/**
	 * Generate the XML representation for this message.
	 * @return the XML String
	 * @throws KVException if not enough data is available to generate a valid KV XML message
	 */
	public String toXML() throws KVException {
		try{
			Element rootEle, keyEle, valueEle, msgEle, tpcIdEle;
			
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
					
					if(tpcOpId != null){
						tpcIdEle = doc.createElement("TPCOpId");
						tpcIdEle.appendChild(doc.createTextNode(tpcOpId));
						rootEle.appendChild(tpcIdEle);
					}
				}
				
				if (msgType.equals("delreq")){
					if(tpcOpId != null){
						tpcIdEle = doc.createElement("TPCOpId");
						tpcIdEle.appendChild(doc.createTextNode(tpcOpId));
						rootEle.appendChild(tpcIdEle);
					}
				}
			} else if (msgType.equals("ready") || msgType.equals("commit") || msgType.equals("abort") || msgType.equals("ack") ){
				
				if(msgType.equals("abort")){
					if(message != null){
						msgEle = doc.createElement("Message");
						msgEle.appendChild(doc.createTextNode(message));
						rootEle.appendChild(msgEle);
					}
				}
				
				if(tpcOpId == null){
					throw new KVException(
							new KVMessage("resp", "Unknown Error: Not enough data available to generate a valid XML message"));
				}
				tpcIdEle = doc.createElement("TPCOpId");
				tpcIdEle.appendChild(doc.createTextNode(tpcOpId));
				rootEle.appendChild(tpcIdEle);
				
			} else if (msgType.equals("register")){
				if(message == null){
					throw new KVException(new KVMessage("resp", "Unknown Error: Not enough data available to generate a valid XML message"));
				}
				msgEle = doc.createElement("Message");
				msgEle.appendChild(doc.createTextNode(message));
				rootEle.appendChild(msgEle);			
			} else if (msgType.equals("resp")){
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
			throw new KVException(new KVMessage("resp", "Unknown Error: Could not send data"));
		} catch (TransformerException tfe){
			throw new KVException(new KVMessage("resp", "Unknown Error: Could not send data"));
		}
	}
	
	public void sendMessage(Socket sock) throws KVException {
		OutputStream out = null;
		try {
			out = sock.getOutputStream();
			String temp = toXML();
			out.write(temp.getBytes("UTF-8"));
			out.flush();
		} 
		catch (IOException e){
			throw new KVException(new KVMessage("resp","Network Error: Could not send data"));
		} 
	}
	
	public void sendMessage(Socket sock, int timeout) throws KVException {
		/*
		 * As was pointed out, setting a timeout when sending the message (while would still technically work),
		 * is a bit silly. As such, this method will be taken out at the end of Spring 2013.
		 */
		// TODO: optional implement me
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof KVMessage)) {
			return false;
		}
		KVMessage oth = (KVMessage) other;
		return (key == oth.key || key != null && key.equals(oth.key)) &&
				(message == oth.message || message != null && message.equals(oth.message)) &&
				(msgType == oth.msgType || msgType != null && msgType.equals(oth.msgType)) &&
				(tpcOpId == oth.tpcOpId || tpcOpId != null && tpcOpId.equals(oth.tpcOpId)) &&
				(value == oth.value || value != null && value.equals(oth.value)); 
	}
	
	@Override
	public String toString() {
		try {
			return toXML();
		} catch (KVException e) {
			e.printStackTrace();
			return super.toString();
		}
	}
}

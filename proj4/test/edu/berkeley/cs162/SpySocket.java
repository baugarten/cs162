package edu.berkeley.cs162;

import java.net.Socket;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class SpySocket extends Socket {
	private String msg;
	private ByteArrayOutputStream output;
	
	public void setKVMessage(KVMessage kvMsg) {
		try {
			msg = kvMsg.toXML();
		} catch (KVException e) {
			// swallow it, bitch
		}
	}
	
	@Override
	public InputStream getInputStream() {
		try {
			return new ByteArrayInputStream(this.msg.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	@Override
	public OutputStream getOutputStream() {
		output = new ByteArrayOutputStream();
		return output;
	}
	
	public KVMessage getOutputMessage() {
		try {
			return new KVMessage(new ByteArrayInputStream(output.toString().getBytes("UTF-8")));
		} catch (Exception e) {
			return null;
		}
	}

}

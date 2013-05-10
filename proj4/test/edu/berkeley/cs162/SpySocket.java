package edu.berkeley.cs162;

import java.net.Socket;
import java.util.Scanner;
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
		System.out.println("OutputStream requested");
		output = new ByteArrayOutputStream() {
			@Override
			public void write(byte[] b) throws IOException {
				Scanner s = new Scanner(new ByteArrayInputStream(b), "UTF-8").useDelimiter("\\A");
				String write = s.hasNext() ? s.next() : "";
				System.out.println("Writing " + write);
				super.write(b);
				
			}
		};
		return output;
	}
	
	public KVMessage getOutputMessage() {
		try {
			String out= output.toString();
			System.out.println("getOutputMessage");
			return new KVMessage(new ByteArrayInputStream(out.getBytes("UTF-8")));
		} catch (Exception e) {
			return null;
		}
	}

}

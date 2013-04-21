package edu.berkeley.cs162;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class UnreadableSocket extends Socket {
	private int callcount = 0;
	@Override
	public InputStream getInputStream() throws IOException {
		if (callcount == 0) {
			callcount++;
			return new UnreadableStream();
		}
		return super.getInputStream();
	}
	
	private static class UnreadableStream extends InputStream {

		@Override
		public int read() throws IOException {
			throw new IOException();
		}
		
	}
}

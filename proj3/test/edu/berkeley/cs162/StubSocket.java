package edu.berkeley.cs162;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;


public class StubSocket extends Socket{
	
	private PipedInputStream input;
	private PipedOutputStream output;

	StubSocket(PipedInputStream in) throws IOException{
		super();
		input = in;
		output = new PipedOutputStream (input);
	}
	
	@Override
	public PipedOutputStream getOutputStream(){
		return output;
	}
	
}

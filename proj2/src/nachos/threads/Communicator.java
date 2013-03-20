package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */


public class Communicator {
	/**
	 * Allocate a new communicator.
	 */

	public Communicator() {
		mutex=new Lock();
		sleepingSpeakers=new Condition(mutex);
		sleepingListeners=new Condition(mutex);
		currentSpeaker=new Condition(mutex);
		occupied=false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word) {
		mutex.acquire();
		
		while(occupied){
			sleepingSpeakers.sleep();
		}
		occupied=true;
		transfer=word;
		sleepingListeners.wake();
		currentSpeaker.sleep();
		mutex.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() {
		mutex.acquire();
		
		while(!occupied){
			sleepingListeners.sleep();
		}
		int temp= transfer;
		occupied=false;
		
		currentSpeaker.wake();
		sleepingSpeakers.wake();
		mutex.release();
		return temp;
	}
	

	private Lock mutex;	
	private Condition  sleepingSpeakers;
	private Condition sleepingListeners;
	private Condition currentSpeaker;
	private boolean occupied;
	private int transfer;
}

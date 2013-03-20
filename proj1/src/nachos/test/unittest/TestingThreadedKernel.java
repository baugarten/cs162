package nachos.test.unittest;

import java.util.concurrent.BlockingQueue;

import nachos.machine.FileSystem;
import nachos.machine.Machine;
import nachos.threads.Alarm;
import nachos.threads.KThread;
import nachos.threads.Scheduler;
import nachos.threads.ThreadedKernel;

public class TestingThreadedKernel extends ThreadedKernel {

	private static final Object DUMMY = new Object();
	private static BlockingQueue<Runnable> instructionQueue;
	private static BlockingQueue<Object> messageQueue;

	public TestingThreadedKernel(Scheduler scheduler, Alarm alarm,
			BlockingQueue<Runnable> instructionQueue,
			BlockingQueue<Object> messageQueue) {
		this(scheduler, alarm, null, instructionQueue, messageQueue);
	}

	public TestingThreadedKernel(Scheduler scheduler, Alarm alarm,
			FileSystem filesystem, BlockingQueue<Runnable> instructionQueue,
			BlockingQueue<Object> messageQueue) {
		ThreadedKernel.scheduler = scheduler;
		ThreadedKernel.alarm = alarm;
		ThreadedKernel.fileSystem = filesystem;
		TestingThreadedKernel.instructionQueue = instructionQueue;
		TestingThreadedKernel.messageQueue = messageQueue;
	}

	public void initialize(String[] args) {

		// start threading
		new KThread(null);

		Machine.interrupt().enable();
	}

	public void run() {
		messageQueue.offer(DUMMY);
		Thread.yield();
		while (true) {
			try {
				Runnable instructions = instructionQueue.take();
				instructions.run();
				messageQueue.offer(DUMMY);
				Thread.yield();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Machine.terminate(e);
			}
		}
	}

}

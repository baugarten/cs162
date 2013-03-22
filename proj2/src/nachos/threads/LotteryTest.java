package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;

import nachos.machine.Machine;
import nachos.threads.LotteryScheduler.LotteryQueue;
import nachos.threads.LotteryScheduler.ThreadState;

public class LotteryTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}
	
	/* The grand daddy method to test the entire suit */
	public static void testAll(){
		testPickingThreads();
		testSimple();
		testAddOrSubtract();
	}
	
	
	// Test the functionality of picking random threads proportional to the
	// number of tickets
	public static void testPickingThreads() {
		Integer val;
		ThreadState thread = null;
		LotteryScheduler scheduler = new LotteryScheduler();
		LotteryQueue queue = (LotteryQueue) scheduler.newThreadQueue(true);
		KThread thread1 = new KThread().setName("Thread1");
		KThread thread2 = new KThread().setName("Thread2");
		KThread thread3 = new KThread().setName("Thread3");

		Machine.interrupt().disable();
		scheduler.setPriority(thread1, 1);
		scheduler.setPriority(thread2, 3);
		scheduler.setPriority(thread3, 6);
		queue.waitForAccess(thread1);
		queue.waitForAccess(thread2);
		queue.waitForAccess(thread3);
		Machine.interrupt().enable();

		System.out
				.println("-------- Begin Lottery Functionality Test---------");
		System.out.println("Thread1's number of tickets = 1");
		System.out.println("Thread2's number of tickets = 3");
		System.out.println("Thread3's number of tickets = 6");
		System.out.println("Sum of queue = " + queue.getSum());
		System.out.println("Size of queue = " + queue.size());

		HashMap<ThreadState, Integer> result = new HashMap<ThreadState, Integer>();
		for (int i = 0; i < 1000; i++) {
			thread = queue.pickNextThread();
			if (thread == null) {
				System.out.println("This line should not be printed at all");
			}
			if (result.containsKey(thread)) {
				val = (Integer) result.get(thread);
				result.put(thread, new Integer(val.intValue() + 1));
			} else {
				result.put(thread, new Integer(0));
			}
		}

		Iterator<ThreadState> iter = result.keySet().iterator();
		while (iter.hasNext()) {
			thread = iter.next();
			System.out.println(thread.thread.getName() + " is picked "
					+ result.get(thread).intValue() + " times");
		}

		System.out.println("---------- End Lottery Test---------");

	}

	// Test ticket donation and priority inversion
	public static void testSimple() {
		final int[] a = { 1 };
		final Lock lock = new Lock();
		final Condition2 con_var = new Condition2(lock);
		final KThread donor = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				while (a[0] > 0) {
					con_var.sleep();
				}
				con_var.wake();
				System.out.println("Donor finishes.");
				lock.release();
			}
		});
		final KThread middle = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				while (a[0] > 0) {
					con_var.sleep();
				}
				con_var.wake();
				System.out.println("Middle finishes.");
				lock.release();
			}
		});
		final KThread receiver = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				a[0] = 0;
				con_var.wakeAll();
				System.out.println("Receiver finishes.");
				lock.release();
			}
		});

		System.out.println("-------- LotteryScheduler Test: Ticket donation and priority inversion --------");
		System.out.println("Expected Receiver to finish first.");

		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(receiver, 10);
		ThreadedKernel.scheduler.setPriority(donor, 300);
		ThreadedKernel.scheduler.setPriority(middle, 1000);
		Machine.interrupt().enable();
		receiver.fork();
		donor.fork();
		middle.fork();
		ThreadedKernel.alarm.waitUntil(100000);
		System.out.println("-------- End testing --------");
	}
	
	// Test increasePriority and decreasePriority
	public static void testAddOrSubtract(){
		final int[] a = { 1 };
		final int[] b = { 20 };
		final Lock lock = new Lock();
		final Condition2 con_var = new Condition2(lock);
		final KThread decreaser = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				while (b[0] > 1) {
					Machine.interrupt().disable();
					ThreadedKernel.scheduler.decreasePriority();
					System.out.println("Number of tickets of Decreaser = " + ThreadedKernel.scheduler.getPriority());
					Machine.interrupt().enable();
					b[0] -= 1;
					con_var.wake();
					con_var.sleep();
				}
				con_var.wake();
				System.out.println("Decreaser finishes.");
				lock.release();
			}
		});
		
		final KThread increaser = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				while (a[0] < 20) {
					Machine.interrupt().disable();
					ThreadedKernel.scheduler.increasePriority();
					System.out.println("Number of tickets of Increaser = " + ThreadedKernel.scheduler.getPriority());
					Machine.interrupt().enable();
					a[0] += 1;
					con_var.wake();
					con_var.sleep();
				}
				con_var.wake();
				System.out.println("Increaser finishes.");
				lock.release();
			}
		});
		
		System.out.println("-------- LotteryScheduler Test: increasePriority and decreasePriority --------");

		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(increaser, 1);
		ThreadedKernel.scheduler.setPriority(decreaser, 20);
		System.out.println("Starting number of tickets of increaser = 1");
		System.out.println("Starting number of tickets of decreaser = 20");
		Machine.interrupt().enable();
		
		increaser.fork();
		decreaser.fork();
		
		ThreadedKernel.alarm.waitUntil(10000);
		System.out.println("------------------------------- End Test -------------------------------------");
	}
	
	public static void testYield(){
		final int[] b = { 10 };
		final KThread decreaser = new KThread(new Runnable() {
			public void run() {
				while (b[0] >= 0) {
					Machine.interrupt().disable();
					System.out.println(b[0]);
					b[0] -= 1;
					KThread.yield();
				}
				System.out.println("Decreaser finishes.");
			}
		});
		
		System.out.println("-------- LotteryScheduler Test: KThread.yield() --------");
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(decreaser, 10);
		Machine.interrupt().enable();
		decreaser.fork();
		ThreadedKernel.alarm.waitUntil(10000);
	}

}

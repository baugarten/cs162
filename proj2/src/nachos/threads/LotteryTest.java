package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.LotteryScheduler.LotteryQueue;
import nachos.threads.LotteryScheduler.ThreadState;

/**This class is the test suit for the LotteryScheduler.
 * There are 4 tests. The testAll function runs all 4 tests
 */
public class LotteryTest {

	/* The grand daddy method to test the entire suit */
	public static void testAll() {
		testPickingThreads();
		testSimpleDonation();
		testAddOrSubtract();
		testYield();
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
	
	// Test increasePriority and decreasePriority
	public static void testAddOrSubtract() {
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
					System.out.println("Number of tickets of Decreaser = "
							+ ThreadedKernel.scheduler.getPriority());
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
					System.out.println("Number of tickets of Increaser = "
							+ ThreadedKernel.scheduler.getPriority());
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

		System.out
				.println("-------- LotteryScheduler Test: increasePriority and decreasePriority --------");

		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(increaser, 1);
		ThreadedKernel.scheduler.setPriority(decreaser, 20);
		System.out.println("Starting number of tickets of increaser = 1");
		System.out.println("Starting number of tickets of decreaser = 20");
		Machine.interrupt().enable();

		increaser.fork();
		decreaser.fork();

		ThreadedKernel.alarm.waitUntil(10000);
		System.out
				.println("------------------------------- End Test -------------------------------------");
	}

	public static void testYield() {
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

		System.out
				.println("-------- LotteryScheduler Test: KThread.yield() --------");
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(decreaser, 10);
		Machine.interrupt().enable();
		decreaser.fork();
		ThreadedKernel.alarm.waitUntil(10000);
		System.out.println("---------------------------End Test -------------------------");
	}

	public static void testSimpleDonation() {
		final int[] a = { 1 };
		final Lock lock = new Lock();
		final KThread donor = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				Machine.interrupt().disable();
				Lib.assertTrue( 40000 == ThreadedKernel.scheduler.getEffectivePriority(), "EP of Donor should be 40000.");
				System.out.println("EP of the Donor = "	+ ThreadedKernel.scheduler.getEffectivePriority()
						+ "\t(This should be 40000)");
				Machine.interrupt().enable();
				System.out.println("Donor finishes.");
				lock.release();
			}
		});
		final KThread middle = new KThread(new Runnable() {
			public void run() {

				while (a[0] > 0) {
					KThread.yield();
					System.out.println("Middle wakes up and runs.");
				}

				Machine.interrupt().disable();
				Lib.assertTrue( 600 == ThreadedKernel.scheduler.getEffectivePriority(), "EP of Middle should be 600.");
				System.out.println("EP of the Middle = " + ThreadedKernel.scheduler.getEffectivePriority()
						+ "\t(This should be 600)");
				Machine.interrupt().enable();
				System.out.println("Middle finishes.");
			}
		});
		final KThread receiver = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				Machine.interrupt().disable();

				donor.fork();
				middle.fork();

				System.out.println("EP of the Receiver(aka LockHolder) when no one is waiting for the lock = "
								+ ThreadedKernel.scheduler.getEffectivePriority() + "\t(This should be 40)");
				Machine.interrupt().enable();
				KThread.yield();
				a[0] = 0;
				Machine.interrupt().disable();
				Lib.assertTrue( 40040 == ThreadedKernel.scheduler.getEffectivePriority(), "EP of Receiver when Donor is waiting for the lock should be 40040.");
				System.out.println("EP of the Receiver(aka LockHolder) when Donor is waiting for the lock = " 
								+ ThreadedKernel.scheduler.getEffectivePriority() + "\t(This should be 40040)");
				Machine.interrupt().enable();
				System.out.println("Receiver finishes.");
				lock.release();
			}
		});

		System.out.println("-------- LotteryScheduler Test: Ticket donation and priority inversion --------");
		System.out.println("There are 3 threads: Receiver, Donor, and Middle.");
		System.out.println("Receiver's # of tickets = 40; Donor's # of tickets = 40000; Middle's # of tickets = 600.");
		System.out.println("Expected result:\n\tReceiver to finish first.\n");

		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(receiver, 40);
		ThreadedKernel.scheduler.setPriority(donor, 40000);
		ThreadedKernel.scheduler.setPriority(middle, 600);
		Machine.interrupt().enable();
		receiver.fork();
		Machine.interrupt().disable();
		Lib.assertTrue( 1 == ThreadedKernel.scheduler.getEffectivePriority(), "EP of the main thread should be 1.");
		System.out.println("EP of the main thread = " + ThreadedKernel.scheduler.getEffectivePriority()
						+ "\t(This should be 1 because the ReadyQueue has no priority donation)");
		Machine.interrupt().enable();
		ThreadedKernel.alarm.waitUntil(10000000);
		System.out.println("-------- End testing --------");
	}
}

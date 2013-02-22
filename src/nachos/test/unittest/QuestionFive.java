package nachos.test.unittest;

import nachos.machine.Machine;
import nachos.threads.Alarm;
import nachos.threads.Condition;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.PriorityScheduler.ThreadState;
import nachos.threads.RoundRobinScheduler;
import nachos.threads.Scheduler;

import org.junit.Assert;
import org.junit.Test;

public class QuestionFive extends TestHarness {
	@Test
	public void testNachos() {
		Assert.assertTrue(true);
	}
	@Test
	public void testNormalPriority() {
		System.out.println("TestNormalPriority");
		enqueueJob(new Runnable() {
			public void run() {
				final Lock contention = new Lock();
				final Lock wakerupper = new Lock();
				final Condition done = new Condition(contention);
				final Condition checkT1Priority = new Condition(wakerupper);
				final Alarm alarm = new Alarm();
				final KThread thread2 = new KThread(new Runnable() {
					@Override
					public void run() {
						// Make sure this one runs last
						alarm.waitUntil(5);
						
						contention.acquire();
						wakerupper.acquire();
						checkT1Priority.wake();
						checkT1Priority.sleep();
						done.wake();
						wakerupper.release();
						contention.release();
					}
				});
				final KThread thread1 = new KThread(new Runnable() {
					@Override
					public void run() {
						// This one should run second
						contention.acquire();
						wakerupper.acquire();
						
						// Thread2 should be waiting on contention by now, lets check
						// my priority
						checkT1Priority.wake();
						checkT1Priority.sleep();
						
						// Release my locks
						wakerupper.release();
						contention.release();
					}
				});
				final KThread threadChecker = new KThread(new Runnable() {
					public void run() {
						// This one runs first
						wakerupper.acquire();
						
						// When T1 is ready, we'll check its priority
						checkT1Priority.sleep();
						
						Machine.interrupt().disable();
						contention.getWaitQueue().print();
						Machine.interrupt().enable();
						Assert.assertTrue(((ThreadState)thread1.schedulingState).getEffectivePriority() == 
								((ThreadState)thread2.schedulingState).getPriority());
						Assert.assertTrue(contention.getWaitQueue().contains(thread2));
						Assert.assertFalse(contention.getWaitQueue().contains(thread1));
						
						// Wake up T1 again, and then sleep until its released its lock
						checkT1Priority.wake();
						checkT1Priority.sleep();
						
						// Check to make sure priority was undonated
						Assert.assertTrue(((ThreadState)thread1.schedulingState).getEffectivePriority() == 
								((ThreadState)thread1.schedulingState).getPriority());
						Assert.assertFalse(contention.getWaitQueue().contains(thread2));
						Assert.assertFalse(contention.getWaitQueue().contains(thread1));
						checkT1Priority.wake();
						
						wakerupper.release();
					}
				});
				
				threadChecker.fork();
				thread1.fork();
				thread2.fork();
				contention.acquire();
				done.sleep();
				Assert.assertFalse(contention.getWaitQueue().contains(thread2));
				Assert.assertFalse(contention.getWaitQueue().contains(thread1));
				System.out.println("We're done");
				contention.release();
			}
		});
	}
}

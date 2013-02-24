package nachos.test.unittest;

import static org.junit.Assert.assertTrue;
import nachos.threads.Condition2;
import nachos.threads.KThread;
import nachos.threads.Lock;

import org.junit.Test;

public class Condition2Test extends TestHarness{

	@Test
	public void testRunNachos(){
		assertTrue(true);
	}
	
	@Test
	public void testCondition2() throws Exception {
		System.out.println("\n--------------------------------------\nTest Condition2\n");
		
		/* Case1: one thread sleeps and another wakes it */
		enqueueJob(new Runnable(){
			@Override
			public void run(){
				final Lock lock = new Lock();
				final Condition2 con_var = new Condition2(lock);
				final KThread sleeper = new KThread(new Sleeper(con_var, lock));
				final KThread waker = new KThread(new Waker(con_var, lock));
				System.out.println("\n--Case1: one thread sleeps and another wakes it--");
				sleeper.fork();
				waker.fork();
			}
			
		});
		
	}

	private static class Sleeper implements Runnable {
		private Condition2 con_var;
		private Lock lock;
		
		Sleeper(Condition2 con, Lock locK){
			con_var = con;
			lock = locK;
		}
		
		public void run(){
			lock.acquire();
			System.out.println("Sleeper: before sleep");
			con_var.sleep();
			System.out.println("Sleeper: after sleep");
			lock.release();
		}
	}
	
	private static class Waker implements Runnable {
		private Condition2 con_var;
		private Lock lock;
		
		Waker(Condition2 con, Lock locK){
			con_var = con;
			lock = locK;
		}
		
		public void run(){
			lock.acquire();
			System.out.println("Waker: before wake");
			con_var.wakeAll();
			System.out.println("Waker: after wake");
			lock.release();
		}
	}
}
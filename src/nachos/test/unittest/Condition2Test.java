package nachos.test.unittest;

import nachos.threads.Condition2;
import nachos.threads.KThread;
import nachos.threads.Lock;

public class Condition2Test{
	private static int item;
	private static Lock lock = new Lock();
	
	public Condition2Test(){
		test1();
	}
	
	public void test1(){
		item = 0;
		Condition2 sleepingConsumers = new Condition2(lock);
		KThread consumer = new KThread(new Consumer(sleepingConsumers, lock));
		Producer producer = new Producer(sleepingConsumers, lock);
		System.out.println("\n--Case1: one thread sleeps and another wakes it--");
		consumer.fork();
		//producer.run();
	}
	

	private static class Consumer implements Runnable {
		private Condition2 con_var;
		private Lock lock;
		
		Consumer(Condition2 con, Lock locK){
			con_var = con;
			lock = locK;
		}
		
		public void run(){
			lock.acquire();
			while(item < 1){
				System.out.println("Consumer: no item so I wait.");
				con_var.sleep();
			}
			item -= 1;
			System.out.println("Consumer: I take away an item.");
			lock.release();
		}
	}
	
	private static class Producer implements Runnable {
		private Condition2 con_var;
		private Lock lock;
		
		Producer(Condition2 con, Lock locK){
			con_var = con;
			lock = locK;
		}
		
		public void run(){
			lock.acquire();
			item += 1;
			System.out.println("Producer: I produced an item.");
			con_var.wakeAll();
			lock.release();
		}
	}
}
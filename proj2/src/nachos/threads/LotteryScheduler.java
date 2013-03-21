package nachos.threads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;
import nachos.threads.PriorityScheduler.ThreadWaiter;


/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	return new LotteryQueue(transferPriority);
    }
	
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}
	
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}
	
	// Test the functionality of picking the next thread as a lottery
	public static void lotteryTest(){
		Integer val;
		ThreadState thread = null;
		LotteryScheduler scheduler = new LotteryScheduler();
		LotteryQueue queue = (LotteryQueue)scheduler.newThreadQueue(true);
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
		
		System.out.println("-------- Begin Lottery Functionality Test---------");
		System.out.println("Thread1's number of tickets = 1");
		System.out.println("Thread2's number of tickets = 3");
		System.out.println("Thread3's number of tickets = 6");
		System.out.println("Sum of queue = " + queue.getSum());
		System.out.println("Size of queue = " + queue.size());
		
	    HashMap<ThreadState, Integer> result = new HashMap<ThreadState, Integer>();
		for (int i = 0; i < 1000; i++){
			thread = queue.pickNextThread();
			if(thread == null){
				System.out.println("This line should not be printed at all");
			}
			if(result.containsKey(thread)){
				val = (Integer)result.get(thread);
				result.put(thread, new Integer(val.intValue()+ 1));
			}
			else {
				result.put(thread, new Integer(0));
			}
		}
		
		Iterator iter = result.keySet().iterator();
		while(iter.hasNext()){
			thread = (ThreadState)iter.next();
			System.out.println(thread.thread.getName() + " is picked " + result.get(thread).intValue() + " times");
		}
		System.out.println("---------- End Lottery Test---------");
		
	}
	
	// Test ticket donation and priority inversion
		public static void selfTest(){
			final int[] a = {1};
			final Lock lock = new Lock();
			final KThread donor = new KThread(new Runnable (){
				public void run(){
					lock.acquire();
					/*
					Machine.interrupt().disable();
					System.out.print("ReadyQueue size when Donor runs = ");
					System.out.println(ThreadedKernel.scheduler.getEffectivePriority());
					Machine.interrupt().enable();
					*/
					System.out.println("Donor finishes.");
					
					//a[0] = 0;
					lock.release();
				}
			});
			final KThread middle = new KThread(new Runnable() {
				public void run(){
					
					while(a[0] == 1){
						KThread.yield();
						System.out.println("Middle woken up");
					}
					/*
					Machine.interrupt().disable();
					System.out.print("ReadyQueue size when middle runs = ");
					System.out.println(ThreadedKernel.scheduler.getEffectivePriority());
					Machine.interrupt().enable();
					*/
					System.out.println("Middle finishes.");
					
				}
			});
			final KThread receiver = new KThread(new Runnable(){
				public void run(){
					lock.acquire();
					donor.fork();
					middle.fork();
					KThread.yield();
					/*
					Machine.interrupt().disable();
					System.out.print("ReadyQueue size when receveir runs = ");
					System.out.println(ThreadedKernel.scheduler.getEffectivePriority());
					Machine.interrupt().enable();
					*/
					a[0] = 0;
					System.out.println("Receiver finishes.");
					lock.release();
				}
			});
			
			System.out.println("-------- LotteryScheduler Test: Ticket donation and priority inversion --------");
			System.out.println("Expected Receiver to finish first.");
			
			Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(receiver, 2);
			ThreadedKernel.scheduler.setPriority(donor, 100);
			ThreadedKernel.scheduler.setPriority(middle, 100);
			Machine.interrupt().enable();
			receiver.fork();
			//donor.fork();
			//middle.fork();
			/*
			Machine.interrupt().disable();
			System.out.print("ReadyQueue size after 3 forks = ");
			System.out.println(ThreadedKernel.scheduler.getEffectivePriority());
			Machine.interrupt().enable();
			*/
			ThreadedKernel.alarm.waitUntil(10000);
			System.out.println("-------- End testing --------");
		}
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 1;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = Integer.MAX_VALUE;
	
	
    protected class LotteryQueue extends ThreadQueue{
    	private java.util.HashMap<ThreadState,Integer> waitQueue ;
    	boolean transferPriority;
    	private ThreadState acquired;
    	private int sum;
    	boolean sumChange, starter, valid;

		LotteryQueue(boolean transferPriority) {
			waitQueue = new java.util.HashMap<ThreadState, Integer>();
			this.transferPriority = transferPriority;
			sum = 0;
			sumChange = true;
			starter = true;
			valid = false;
		}
		
		// DONE
		public void newSum(int newsum, ThreadState ts) {
			//int oldSum = this.sum;
			if (newsum < 0){
				sum = 0;
			}
			else {
				sum = newsum;
			}
			
			if(this.acquired != null){
				this.acquired.priorityChange(this.sum);
			}
			/* For now, no telling the acquired the queue has changed
			if (this.acquired != null){
				if(ts == acquired && sum > oldSum) {
					this.acquired.setEffectivePriority(sum - oldSum);
					//this.acquired.priorityChange(this.sum);
				}
				else{
				this.acquired.priorityChange(this.sum);
				}
			}
			*/
		}
		
		private void announce(){
			starter = true;
			this.acquired.priorityChange(sum);
		}
		
		private void announceSumChange(ThreadState ts){
			if (this.acquired != null && starter == false){
				announce();
				starter = false;
			}
		}
		
		//DONE
		public void updateWaitingThread(ThreadState ts) {
			Lib.assertTrue(waitQueue.containsKey(ts));
			
			int effectivePriority = ts.getEffectivePriority();
			int oldEffectivePrio = waitQueue.get(ts).intValue();
			if (oldEffectivePrio == effectivePriority) {
				return;
			}
			
			//ts.setValid();
			// update the queue
			waitQueue.put(ts,new Integer(effectivePriority));
			// update the sum of the lottery queue
			/*
			if(acquired != ts){
				newSum(getSum() - oldEffectivePrio + effectivePriority, ts);
			}
			*/
			// flag the queue that its sum has changed
			sumChange = true;
			
			announceSumChange(ts);
				
		}
		
		//DONE
		public void dequeueWaitingThread(ThreadState ts) {
			Lib.assertTrue(waitQueue.containsKey(ts));
			
			// remove the thread from the lottery queue
			waitQueue.remove(ts);
			
			// update new sum of the lottery queue
			//newSum(getSum() - ts.getEffectivePriority(), ts);
			
			// flag the queue that its sum has changed
			sumChange = true;
			announceSumChange(ts);
		}
		
		// DONE
		public void enqueueWaitingThread(ThreadState ts) {
			int effectivePriority = ts.getEffectivePriority();

			Lib.assertTrue(!waitQueue.containsKey(ts));
			
			// put thread on queue
			if(acquired == ts){
				waitQueue.put(ts, new Integer(effectivePriority - getSum()));
			} else {
				waitQueue.put(ts, new Integer(effectivePriority));
			}
			
			ts.waitForAccess(this);
			//ts.isValid();
			
			// update sum of lottery queue
			/*
			if(acquired != ts){
				newSum(getSum() + effectivePriority, ts);
			}
			else {
				newSum(effectivePriority, ts);
			}
			*/
			
			// flag the queue that its sum has changed
			sumChange = true;
			announceSumChange(ts);
		}
		
		// add a thread to the queue - DONE
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);
			
			enqueueWaitingThread(ts);
		}
		
		
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			ThreadState ts = getThreadState(thread);

			// unacquire previous
			if (acquired != null) {
				acquired.unacquire(this);
			}
			
			// if thread on waiting queue, dequeue it
			if (waitQueue.containsKey(ts)) {
				dequeueWaitingThread(ts);
			}
			
			this.acquired = ts;
			ts.acquire(this);

		}
		
		public int size(){
			return waitQueue.size();
		}
		
		// DONE
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());

			ThreadState ts = pickNextThread();
			
			if (ts == null) {
				if (acquired != null) {
					acquired.unacquire(this);
				}
				return null;
			}
			acquire(ts.thread);
			
			return ts.thread;
		}
		
		// hold a lottery - DONE
		protected ThreadState pickNextThread() {
			int total = 0;
			int num;
			ThreadState result = null;
			Random rand = new Random();
			if (waitQueue.size() == 0){
				return null;
			}
			
			if (sumChange){
				updateSum();
			}
			
			num = rand.nextInt(sum) + 1;
			Iterator<ThreadState> iter = waitQueue.keySet().iterator();
			while(num > total && iter.hasNext()){
				result = iter.next();
				total += waitQueue.get(result).intValue();
			}
			return result;
		}
    	
		// DONE
		public int getSum(){
			if (sumChange == false){
				return sum;
			} else {
				updateSum();
				return sum;
			}
		}
		
		// DONE
		private void updateSum(){
			Lib.assertTrue(sumChange == true);
			int total = 0;
			Map.Entry<ThreadState, Integer> value;
			Iterator<Map.Entry<ThreadState, Integer>> iter = waitQueue.entrySet().iterator(); 
			while(iter.hasNext()){
				value = iter.next();
				if(value.getKey().isValid()){
					total += value.getValue().intValue();
				} else {
					updateWaitingThread(value.getKey());
				}
			}
			sum = total;
			sumChange = false;
			valid = true;
		}
		
		public boolean contains(KThread thread) {
			ThreadState ts = getThreadState(thread);
			return waitQueue.containsKey(ts);
		}
		
		public void print(){
			Lib.assertTrue(Machine.interrupt().disabled());
			System.out.println("Number of tickets = " + getSum() + "\tNumber of threads on queue = " + size());
			System.out.println(waitQueue.toString());
		}
		
		public boolean empty() {
			return waitQueue.size() == 0;
		}
		
		public boolean isValid(){
			return !sumChange;
		}
    }
    
    public class ThreadState extends PriorityScheduler.ThreadState{
    	private boolean updatedEffectivePrio;
    	private boolean starter;
		public ThreadState(KThread thread){
    		super(thread);
    		updatedEffectivePrio = true;
    		starter = true;
    	}
    	
		// DONE
		public int getEffectivePriority(){
			if(!updatedEffectivePrio){
				return calculateEffectivePriority();
			}
			else {
				return effectivePriority;
			}
		}
		
		// DONE
		public void priorityChange(int newPri) {
			updatedEffectivePrio = false;
			if(!starter){
				announcePrioChange();	
				starter = false;
			}
		}
		
		public void setEffectivePriority(int value){
			Lib.assertTrue(this.priority >= value);
			this.effectivePriority = value;
			announcePrioChange();
		}
		
		// DONE
    	public int calculateEffectivePriority() {
    		
    			//starter = true;
    			int sum = priority;
    			int queueSum = 0;
    			Iterator<ThreadQueue> iter = acquiredpqs.iterator();
    			LotteryQueue queue;
    			while(iter.hasNext()){
    				queue = (LotteryQueue) iter.next();
    				queueSum = queue.getSum();
    				if (Integer.MAX_VALUE - sum < queueSum){
    					return Integer.MAX_VALUE;
    				}
    				sum += queueSum;
    			}
    		updatedEffectivePrio = true;
			return sum;
		}
    	
    	// DONE
    	void effectivePriorityUpdated() {
    		
			int newEffectivePriority = calculateEffectivePriority();
			if (newEffectivePriority != effectivePriority) {
				effectivePriority = newEffectivePriority;
				updatedEffectivePrio = false;
				announcePrioChange();
			}
		}
    	
    	// DONE
    	public void waitForAccess(ThreadQueue waitQueue) {
			waitingpqs.add(waitQueue);
		}
    	
    	//DONE
    	void announcePrioChange(){
    		starter = true;
    		LotteryQueue queue;
    		Iterator<ThreadQueue> iter;
    		iter = waitingpqs.iterator();
			while(iter.hasNext()){
				queue = (LotteryQueue) iter.next();
				queue.updateWaitingThread(this);
			}
			starter = false;
    	}
    	
    	// DONE
    	public void acquire(ThreadQueue waitQueue) {
			waitingpqs.remove(waitQueue);
			acquiredpqs.add(waitQueue);
			effectivePriorityUpdated();
		}

    	//DONE
    	public void unacquire(ThreadQueue noQueue) {
			acquiredpqs.remove(noQueue);
			effectivePriorityUpdated();
		}
    	
    	public boolean isValid(){
    		return updatedEffectivePrio;
    	}
    }
}

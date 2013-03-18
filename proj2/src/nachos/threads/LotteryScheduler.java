package nachos.threads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;
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
	
	
    protected class LotteryQueue extends PriorityQueue{
    	java.util.HashMap<ThreadState,Integer> waitQueue = new java.util.HashMap<ThreadState, Integer>();
    	private int sum;

		LotteryQueue(boolean transferPriority) {
			super(transferPriority);
			sum = 0;
		}
		
		public void newSum(int newsum) {
			sum = newsum;
			if (this.acquired != null) {
				this.acquired.priorityChange(this.sum);
			}
		}
		
		public void updateWaitingThread(ThreadState ts) {
			dbg("Update " + ts);
			Lib.assertTrue(waitQueue.containsKey(ts));
			int effectivePriority = ts.getEffectivePriority();
			if (ts.priority == effectivePriority) {
				return;
			}
			// put the thread on the lottery queue
			waitQueue.put(ts, new Integer(effectivePriority));

			// update the sum of the lottery queue
			newSum(getSum() + ts.effectivePriority);
		}
		
		public void dequeueWaitingThread(ThreadState ts) {
			dbg("DQ " + ts);
			Lib.assertTrue(waitQueue.containsKey(ts));
			
			// remove the thread from the lottery queue
			waitQueue.remove(ts);
			
			// update new sum of the lottery queue
			newSum(getSum() - ts.getEffectivePriority());
		}
		
		public void enqueueWaitingThread(ThreadState ts) {
			dbg("EQ " + ts);
			int effectivePriority = ts.getEffectivePriority();

			Lib.assertTrue(!waitQueue.containsKey(ts));
			
			// update the new effectivePriority of the thread on queue
			waitQueue.put(ts,new Integer(effectivePriority));
			
			// update sum of lottery queue
			newSum(getSum() + effectivePriority);
			dbg("EQ Done " + ts);
		}
		
		// add a thread to the queue
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState ts = getThreadState(thread);
			
			enqueueWaitingThread(ts);
			dbg("WFA " + ts);
			ts.waitForAccess(this);
			dbg("WFA Done " + ts);
		}
				
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			LotteryScheduler.ThreadState ts = getThreadState(thread);
			dbg("Acquire " + ts);
			
			// unacquire previous
			if (acquired != null) {
				acquired.unacquire(this);
			}
			
			// if thread on waiting queue, nuke it
			if (waitQueue.containsKey(ts)) {
				dequeueWaitingThread(ts);
				//System.out.println("NUKED");
			}
			
			// set acquired
			this.acquired = ts;
			ts.acquire(this);
			
			dbg("Acquire done " + ts);
		}
		
		public int getSum(){
			return sum;
		}
		
		public int size(){
			return waitQueue.size();
		}
		
		// hold a lottery
		protected ThreadState pickNextThread() {
			int total = 0;
			ThreadState result = null;
			Random rand = new Random();
			int num = rand.nextInt(sum) + 1;
			Iterator<ThreadState> iter = waitQueue.keySet().iterator();
			while(num > total && iter.hasNext()){
				result = iter.next();
				total += waitQueue.get(result).intValue();
			}
			return result;
		}
    	
		public boolean contains(KThread thread) {
			ThreadState ts = getThreadState(thread);
			return waitQueue.containsKey(ts);
		}
		
    }
    
    public class ThreadState extends PriorityScheduler.ThreadState{
    	private HashSet<LotteryQueue> acquiredpqs = new HashSet<LotteryQueue>();
		private HashSet<LotteryQueue> waitingpqs = new HashSet<LotteryQueue>();
    	
		public ThreadState(KThread thread){
    		super(thread);
    	}
    	
    	public int calculateEffectivePriority() {
			int sum = priority;
			int queueSum = 0;
			for (LotteryQueue queue : acquiredpqs) {
				queueSum = queue.getSum();
				if (Integer.MAX_VALUE - sum < queueSum){
					return Integer.MAX_VALUE;
				}
				sum += queueSum;
			}
			return sum;
		}
    	
    }
}

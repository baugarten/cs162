package nachos.threads;

import nachos.machine.*;
import nachos.threads.LotteryScheduler.LotteryQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
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

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		private HashMap<ThreadState, ThreadWaiter> threadWaiterHash = 	// KThread -> threadWaiter on PQ
				new HashMap<ThreadState, ThreadWaiter>(); 
		private java.util.PriorityQueue<ThreadWaiter> waitQueue =
				new java.util.PriorityQueue<ThreadWaiter>();
		protected ThreadState acquired;

		public boolean transferPriority;
		private int max = 0;
		
		private int enqueueOrder = 0;
		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public int getMax() {
			return max;
		}
		
		public void dbg(String str) {
			//System.out.println(this + " " + str);
		}
		
		public void updateWaitingThread(ThreadState ts) {
			dbg("Update " + ts);
			Lib.assertTrue(threadWaiterHash.containsKey(ts));
			
			ThreadWaiter tw = threadWaiterHash.get(ts);
			if (tw.priority == ts.getEffectivePriority()) {
				return;
			}
			
			waitQueue.remove(tw);
			ThreadWaiter twait = new ThreadWaiter(ts,
					ts.getEffectivePriority(), tw.time);
			waitQueue.add(twait);
			threadWaiterHash.put(ts, twait);
			
			ThreadWaiter nextTw = waitQueue.peek();
			newMax(nextTw.priority);
		}
		
		public void newMax(int newmax) {
			max = newmax;
			if (this.acquired != null) {
				this.acquired.priorityChange(this.max);
			}
		}
		
		public void dequeueWaitingThread(ThreadState ts) {
			dbg("DQ " + ts);
			Lib.assertTrue(threadWaiterHash.containsKey(ts));
			
			ThreadWaiter tw = threadWaiterHash.get(ts);
			waitQueue.remove(tw);
			threadWaiterHash.remove(ts);
			
			// check if we need to update priority queue
			int deqPri = tw.priority;
			if (max == deqPri) {
				ThreadWaiter nextTw = waitQueue.peek();
				if (nextTw == null) {
					newMax(0);
				} else {
					newMax(nextTw.priority);
				}
			}
		}
		
		// called when max poetntially gets updated
		public void enqueueWaitingThread(ThreadState ts) {
			dbg("EQ " + ts);
			int effectivePriority = ts.getEffectivePriority();
			
			// check thread isn't already there
			Lib.assertTrue(!threadWaiterHash.containsKey(ts));
			
			// enqueue thread
			enqueueOrder++;
			ThreadWaiter twait = new ThreadWaiter(ts,
					effectivePriority, enqueueOrder);
			waitQueue.add(twait);
			threadWaiterHash.put(ts, twait);
			
			// update max
			if (effectivePriority > this.max) {
				newMax(effectivePriority);
			}
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
			
			ThreadState ts = getThreadState(thread);
			dbg("Acquire " + ts);
			
			// unacquire previous
			if (acquired != null) {
				acquired.unacquire(this);
			}
			
			// if thread on waiting queue, nuke it
			if (threadWaiterHash.containsKey(ts)) {
				dequeueWaitingThread(ts);
				//System.out.println("NUKED");
			}
			
			// set acquired
			this.acquired = ts;
			ts.acquire(this);
			
			dbg("Acquire done " + ts);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			dbg("  NextThread " + waitQueue );
			
			ThreadState ts = pickNextThread();
			
			if (ts == null) {
				if (acquired != null) {
					acquired.unacquire(this);
				}
				
				dbg("  NextThread NULL" );
				return null;
			}
			//System.out.println("expect ");
			acquire(ts.thread);
			
			return ts.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			ThreadWaiter st = waitQueue.peek();
			return st != null ? st.state : null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			dbg(waitQueue.toString());
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */


		@Override
		public boolean contains(KThread thread) {
			ThreadState ts = getThreadState(thread);
			return threadWaiterHash.containsKey(ts);
		}

		@Override
		public boolean empty() {
			return waitQueue.size() == 0;
		}
		
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	public class ThreadState {
		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		
		/**
		 * The list of priority queues for which I'm holding the resource or
		 * waiting for the resource
		 */
		protected HashSet<ThreadQueue> acquiredpqs = new HashSet<ThreadQueue>();
		protected HashSet<ThreadQueue> waitingpqs = new HashSet<ThreadQueue>();
		// - should never be more than one!
		
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.priority = priorityDefault;
			this.effectivePriority = this.priority;
		}

		void effectivePriorityUpdated() {
			PriorityQueue queue;
			Iterator<ThreadQueue> iter;
			int newEffectivePriority = calculateEffectivePriority();
			//System.out.println("  pri update " + this + " " + newEffectivePriority + "/" + effectivePriority);
			if (newEffectivePriority != effectivePriority) {
				effectivePriority = newEffectivePriority;
				iter = waitingpqs.iterator();
				while(iter.hasNext()){
				//for (ThreadQueue queue : waitingpqs) {
					//System.out.println("  update " + queue);
					queue = (PriorityQueue) iter.next();
					queue.updateWaitingThread(this);
					//System.out.println("    done");
					
				}
			}
		}
		
		public void priorityChange(int newPri) {
			effectivePriorityUpdated();
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int calculateEffectivePriority() {
			PriorityQueue queue;
			int max = priority;
			Iterator<ThreadQueue> iter = acquiredpqs.iterator();
			while(iter.hasNext()){
			//for (ThreadQueue queue : acquiredpqs) {
				queue = (PriorityQueue) iter.next();
				max = Math.max(max, queue.getMax());
			}
			return max;
		}
		
		public int getEffectivePriority() {
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			this.priority = priority;
			effectivePriorityUpdated();
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(ThreadQueue waitQueue) {
			waitingpqs.add(waitQueue);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(ThreadQueue waitQueue) {
			waitingpqs.remove(waitQueue);
			acquiredpqs.add(waitQueue);
			effectivePriorityUpdated();
		}

		public void unacquire(ThreadQueue noQueue) {
			acquiredpqs.remove(noQueue);
			effectivePriorityUpdated();
		}
		
	}

	public class ThreadWaiter implements Comparable<ThreadWaiter> {
		protected ThreadState state;
		protected long time;
		protected int priority;

		public ThreadWaiter(ThreadState state, int priority, long time) {
			this.priority = priority;
			this.state = state;
			this.time = time;
		}

		@Override
		public int compareTo(ThreadWaiter other) {
			if (priority > other.priority) {
				return -1;
			} else if (priority == other.priority) {
				if (time > other.time) {
					return 1;
				} else if (time < other.time) {
					return -1;
				}
				return 0;
			}
			return 1;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (!(other instanceof ThreadWaiter))
				return false;
			ThreadWaiter o = (ThreadWaiter) other;
			return this.state.thread.compareTo(o.state.thread) == 0;
		}

		public String toString() {
			return this.state.thread.toString();
		}
		
	}
}

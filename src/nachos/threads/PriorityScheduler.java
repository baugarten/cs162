package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
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
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
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
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

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
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
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
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	    this.pq = new java.util.PriorityQueue<ThreadWaiter>();
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).waitForAccess(this);
	    this.pq.add(new ThreadWaiter(getThreadState(thread), Machine.timer().getTime()));
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    getThreadState(thread).acquire(this);
	    if (this.acquired != null) {
	    	this.acquired.unacquire(this);
	    }
	    this.acquired = getThreadState(thread);
	    this.pq.remove(getThreadState(thread));
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    ThreadWaiter tmp = this.pq.poll();
	    return tmp != null ? tmp.state.thread : null;
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected ThreadState pickNextThread() {
	    return this.pq.peek().state;
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    System.out.println(pq.toString());
	}

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	public boolean transferPriority;

	public int max() {
		int max = 0;
		for (ThreadWaiter st : this.pq) {
			if (st != null && st.state != this.acquired) {
				if (max < st.state.getEffectivePriority()) {
					max = st.state.getEffectivePriority();
				}
			} else if (st != null && max < st.state.getPriority()) {
				max = st.state.getPriority();
			}
		}
		return max;
	}

	public void invalidate() {
		// Nothing for now
	}

	private java.util.PriorityQueue<ThreadWaiter> pq;
	private ThreadState acquired;

	@Override
	public boolean contains(KThread thread2) {
		return pq.contains(new ThreadWaiter(getThreadState(thread2), -1));
	}

	@Override
	public boolean empty() {
		return pq.size() == 0;
	}
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    public class ThreadState {
    	
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
	    
	    this.pqs = new ArrayList<PriorityQueue>();
	    
	    setPriority(priorityDefault);
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
		int max = priority;
		for (PriorityQueue queue : pqs) {
			if (queue.max() > max) {
				max = queue.max();
			}
		}
	    return max;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority) {
	    	return;
	    }
	    
	    this.priority = priority;
	    
	    for (PriorityQueue queue : pqs) {
	    	queue.invalidate(); // invalidate cache
	    }
	}

	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	public void acquire(PriorityQueue waitQueue) {
		this.pqs.add(waitQueue);
	}	
	
	public void unacquire(PriorityQueue noQueue) {
		this.pqs.remove(noQueue);
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
	/** The list of priority queues for which I'm holding the 
	 * resource or waiting for the resource */
    private List<PriorityQueue> pqs;
    
    }
    
    public class ThreadWaiter implements Comparable<ThreadWaiter> {
        protected ThreadState state;
        protected long time;
        public ThreadWaiter(ThreadState state, long time) {
            this.state = state;
            this.time = time;
        }
        @Override
        public int compareTo(ThreadWaiter other) {
            if (this.state.getEffectivePriority() > other.state.getEffectivePriority()) {
                return -1;
            } else if (this.state.getEffectivePriority() == other.state.getEffectivePriority()) {
                if (this.time < other.time) {
                    return 1;
                } else if (this.time > other.time) {
                    return -1;
                }
                return 0;
            }
            return 1;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (!(other instanceof ThreadWaiter)) return false;
            ThreadWaiter o = (ThreadWaiter) other;
            return this.state.thread.compareTo(o.state.thread) == 0;
        }

        public String toString() {
            return this.state.thread.toString();
        }
    }
}

package nachos.threads;

public class KThreadTimer implements Comparable<KThreadTimer> {

	private KThread currentThread;
	private long waitTime;
	
	public KThreadTimer (KThread thread, long time) {
		currentThread = thread;
		waitTime = time;
	}
	
	public long getWaitTime() {
		return waitTime;
	}
	
	public KThread getThread() {
		return currentThread;
	}

	@Override
	public int compareTo(KThreadTimer otherThreadTimer) {
		// TODO Compare KThreads based on time asleep
		if (otherThreadTimer.waitTime < this.waitTime) {
			return 1;		
		} else if (otherThreadTimer.waitTime == this.waitTime) {
			return 0;
		} else {
			return -1;
		}
	}

}

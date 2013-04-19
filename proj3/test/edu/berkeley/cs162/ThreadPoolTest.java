package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.lang.Thread;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ThreadPoolTest {

	private ThreadPool pool = null;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Tests adding 1 job when exactly 1 thread is available
	 */

	@Test
	public void testAddOneThreadOneJob() {
		
		// Initialize thread pool of capacity 1
		pool = new ThreadPool(1);
		
		// Define a runnable thread that waits some time
		Thread thread1 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 1's run method");
						try {
						    Thread.sleep(1000);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Block for some time and now finishing thread 1");
					}
				});
		
		// Add the thread to jobs, expect thread1 to be run immediately by the one worker thread
		try {
			pool.addToQueue(thread1);
		} catch (InterruptedException e) {
			System.out.println("Threw InterruptedException: " + e.getMessage());
		}
		
		// Ensure we still have 1 worker thread and no more jobs
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 0);
		
	}
	
	/**
	 * Test adding two jobs to a pool with one worker thread
	 */
	@Test
	public void testAddOneThreadTwoJobs() {
		
		// Initialize thread pool of capacity 1
		pool = new ThreadPool(1);
		
		// Define a runnable thread1 that waits some time
		Thread thread1 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 1's run method");
						try {
						    Thread.sleep(1000);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 1");
					}
				});
		
		// Define a second runnable thread2 that also waits for some shorter time
		Thread thread2 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 2's run method");
						try {
						    Thread.sleep(500);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 2");
					}
				});
		
		// Add the thread to jobs, expect thread1 to be run immediately by the one worker thread
		// Expect thread2 to be added to jobs
		try {
			pool.addToQueue(thread1);
			pool.addToQueue(thread2);
		} catch (InterruptedException e) {
			System.out.println("Threw InterruptedException: " + e.getMessage());
		}
		
		// Ensure we still have 1 worker thread and 1 more job
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 1);
		
		// Busy wait until we see the worker thread service the job
		while (pool.getNumJobs() != 0) {}
		
		// Ensure we still have 1 worker thread and 0 jobs
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 0);		
	}
	
	/**
	 * Test adding multiple jobs
	 */
	
	@Test
	public void testAddOneThreadMultipleJobs() {
		
		// Initialize thread pool of capacity 1
		pool = new ThreadPool(1);
		
		// Define a runnable thread1 that waits some time
		Thread thread1 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 1's run method");
						try {
						    Thread.sleep(1000);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 1");
					}
				});
		
		// Define a second runnable thread2 that also waits for some shorter time
		Thread thread2 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 2's run method");
						try {
						    Thread.sleep(500);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 2");
					}
				});
		
		// Define a third runnable thread3 that also waits for some time
		Thread thread3 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 3's run method");
						try {
							Thread.sleep(1000);
						} catch(InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 3");
					}
				});
				
		// Define a fourth runnable thread4 that also waits for some time
		Thread thread4 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 4's run method");
						try {
							Thread.sleep(1000);
						} catch(InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 4");
					}
				});

		// Add the thread to jobs, expect thread1 to be run immediately by the one worker thread
		// Expect thread2 to be added to jobs
		try {
			pool.addToQueue(thread1);
			pool.addToQueue(thread2);
			pool.addToQueue(thread3);
			pool.addToQueue(thread4);
		} catch (InterruptedException e) {
			System.out.println("Threw InterruptedException: " + e.getMessage());
		}
		
		// Ensure we still have 1 worker thread and 3 more job
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 3);
		
		// Busy wait until we see the worker thread service the next job
		while (pool.getNumJobs() == 2) {}
		
		// Ensure we still have 1 worker thread and 2 jobs
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 2);	
		
		// Busy wait until we see the worker thread service the next job
		while (pool.getNumJobs() == 1) {}

		// Ensure we still have 1 worker thread and 1 jobs
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 1);	
		
		// Busy wait until we see the worker thread service the next job
		while (pool.getNumJobs() == 0) {}

		// Ensure we still have 1 worker thread and 0 jobs
		assertEquals(pool.getNumWorkerThreads(), 1);
		assertEquals(pool.getNumJobs(), 0);	
	} 
	
	@Test
	public void testAddTwoThreadsOneJob() {
		
		// Initialize thread pool of capacity 1
		pool = new ThreadPool(2);
		
		// Define a runnable thread that waits some time
		Thread thread1 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 1's run method");
						try {
						    Thread.sleep(1000);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Block for some time and now finishing thread 1");
					}
				});
		
		// Add the thread to jobs, expect thread1 to be run immediately by the one worker thread
		try {
			pool.addToQueue(thread1);
		} catch (InterruptedException e) {
			System.out.println("Threw InterruptedException: " + e.getMessage());
		}
		
		// Ensure we still have 2 worker thread and no more jobs
		assertEquals(pool.getNumWorkerThreads(), 2);
		assertEquals(pool.getNumJobs(), 0);
		
	}
	
	@Test
	public void testAddTwoThreadsTwoJobs() {
		
		// Initialize thread pool of capacity 1
		pool = new ThreadPool(2);
		
		// Define a runnable thread that waits some time
		Thread thread1 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 1's run method");
						try {
						    Thread.sleep(1000);
						} catch(InterruptedException ex) {
						    Thread.currentThread().interrupt();
						}
						System.out.println("Block for some time and now finishing thread 1");
					}
				});
		
		// Define a second runnable thread2 that also waits for some shorter time
		Thread thread2 = new Thread(
				new Runnable() {
					public void run() {
						System.out.println("Running thread 2's run method");
						try {
							Thread.sleep(500);
						} catch(InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
						System.out.println("Blocked for some time and now finishing thread 2");
					}
				});
		
		// Add the thread to jobs, expect thread1 to be run immediately by the first worker thread
		// Expect thread2 to be run soon after by the second worker thread
		try {
			pool.addToQueue(thread1);
			pool.addToQueue(thread2);
		} catch (InterruptedException e) {
			System.out.println("Threw InterruptedException: " + e.getMessage());
		}
		
		// Ensure we still have 2 worker thread and no more jobs
		assertEquals(pool.getNumWorkerThreads(), 2);
		assertEquals(pool.getNumJobs(), 0);
		
	}

}

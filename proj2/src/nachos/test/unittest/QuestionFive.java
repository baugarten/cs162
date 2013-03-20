package nachos.test.unittest;

import java.util.Random;

import nachos.machine.Machine;
import nachos.threads.Alarm;
import nachos.threads.Condition;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.ThreadedKernel;
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
    public void testNormalPriority() throws Exception {
        System.out.println("TestNormalPriority");
        enqueueJob(new Runnable() {
            public void run() {
                final Lock contention = new Lock();
                final Lock wakerupper = new Lock();
                final Condition done = new Condition(contention);
                final Condition checkT1Priority = new Condition(wakerupper);
                final Condition waitUntilT2 = new Condition(wakerupper);
                final Condition T2start = new Condition(wakerupper);
                final Alarm alarm = new Alarm();
                final KThread thread2 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure this one runs last
                        contention.acquire();
                        wakerupper.acquire();
                        System.out.println("waitUntilT2.wake()");
                        // waitUntilT2.wakeAll();
                        System.out.println("Thread 2 actually acquire");
                        checkT1Priority.wake();
                        checkT1Priority.sleep();
                        done.wake();
                        wakerupper.release();
                        contention.release();
                        System.out.println("Thread 2 finished");
                        KThread.finish();
                    }
                });
                final KThread thread1 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // This one should run second
                        System.out.println("Thread 1 trying to acquire");
                        contention.acquire();
                        wakerupper.acquire();

                        thread2.fork();
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(thread2, 7);
                        Machine.interrupt().enable();
                        while (contention.getWaitQueue().empty()) {
                            KThread.currentThread().yield();
                        }
                        // Wait until thread2 starts

                        // Thread2 should be waiting on contention by now, lets
                        // check
                        // my priority
                        System.out.println("Testing thread1 priority");
                        checkT1Priority.wake();
                        checkT1Priority.sleep();

                        // Release my locks
                        contention.release();
                        System.out.println("Thread1 lock released");

                        wakerupper.release();

                        KThread.finish();
                    }
                });
                final KThread threadChecker = new KThread(new Runnable() {
                    public void run() {
                        // This one runs first
                        wakerupper.acquire();

                        thread1.fork();
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(thread1, 3);
                        Machine.interrupt().disable();

                        // When T1 is ready, we'll check its priority
                        checkT1Priority.sleep();
                        System.out.println("Checking Priorities 1st time");

                        Machine.interrupt().disable();
                        contention.getWaitQueue().print();
                        Machine.interrupt().enable();
                        Assert.assertEquals(((ThreadState) thread2.schedulingState).getPriority(),
                                ((ThreadState) thread1.schedulingState).getEffectivePriority());
                        Assert.assertTrue(contention.getWaitQueue().contains(thread2));
                        Assert.assertFalse(contention.getWaitQueue().contains(thread1));

                        // Wake up T1 again, and then sleep until its released
                        // its lock
                        checkT1Priority.wake();
                        checkT1Priority.sleep();
                        System.out.println("Checking Priorities 2nd time");

                        // Check to make sure priority was undonated
                        Assert.assertEquals(((ThreadState) thread1.schedulingState).getPriority(),
                                ((ThreadState) thread1.schedulingState).getEffectivePriority());

                        // Niether thread is waiting on contention
                        Assert.assertFalse(contention.getWaitQueue().contains(thread2));
                        Assert.assertFalse(contention.getWaitQueue().contains(thread1));
                        checkT1Priority.wakeAll();

                        wakerupper.release();
                        System.out.println("Checker finished");
                        KThread.finish();
                    }
                });
                thread1.setName("Thread 1");
                thread2.setName("Thread 2");
                threadChecker.setName("Thread Checker");

                contention.acquire();
                threadChecker.fork();
                ((ThreadState) threadChecker.schedulingState).setPriority(8);

                System.out.println("Giving up done");
                done.sleep();
                Assert.assertFalse(contention.getWaitQueue().contains(thread2));
                Assert.assertFalse(contention.getWaitQueue().contains(thread1));
                Assert.assertTrue(thread1.getStatus() == KThread.statusFinished);
                Assert.assertTrue(thread2.getStatus() == KThread.statusFinished);
                System.out.println("We're done");
                contention.release();

                System.out.println("We're done");
            }
        });

        enqueueJob(new Runnable() {
            public void run() {
                System.out.println("PRIORITY INVERSION");
                final Lock contention = new Lock();
                final Lock wakerupper = new Lock();
                final Condition checkLowPriority = new Condition(wakerupper);
                final Condition waitUntilMed = new Condition(wakerupper);
                final Condition done = new Condition(wakerupper);
                final KThread medP = new KThread(new Runnable() {
                    public void run() {
                        System.out.println("Med trying to acquire");
                        wakerupper.acquire();
                        KThread.yield();
                        System.out.println("Waking");
                        waitUntilMed.wake();
                        wakerupper.release();
                        Random r = new Random();
                        System.out.println("Med has long running task");
                        while (true) {
                            if (contention.getWaitQueue().empty()) {
                                System.out.println("Med is finished");
                                wakerupper.acquire();
                                done.wake();
                                wakerupper.release();
                                KThread.finish();
                            }
                            r.nextGaussian();
                            r.nextGaussian();
                            r.nextGaussian();
                            KThread.yield();
                        }
                    }
                });
                final KThread highP = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure this one run third
                        System.out.println("H trying to acquire");
                        contention.acquire(); // Try to acquire lock that L has
                        wakerupper.acquire();

                        System.out.println("H acquired");
                        checkLowPriority.wake();
                        checkLowPriority.sleep();

                        wakerupper.release();
                        contention.release();

                        KThread.finish();
                    }
                });
                final KThread lowP = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        // This one should run second
                        System.out.println("L trying to acquire");
                        contention.acquire();
                        wakerupper.acquire();

                        highP.fork();
                        highP.setName("High");
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(highP, 6);
                        Machine.interrupt().enable();
                        while (contention.getWaitQueue().empty()) {
                            KThread.currentThread().yield();
                        }

                        // Thread2 should be waiting on contention by now, lets
                        // check
                        // my priority
                        checkLowPriority.wake();
                        checkLowPriority.sleep();

                        medP.fork();
                        medP.setName("Med");
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(medP, 4);
                        Machine.interrupt().enable();

                        System.out.println("Sleeping");
                        waitUntilMed.sleep();
                        System.out.println("Med is awake");
                        // Now lets wait until the medium task started
                        while (medP.getStatus() != KThread.statusReady) {
                            System.out.println("Yield");
                            KThread.yield();
                        }
                        System.out.println("Yield");
                        KThread.yield();
                        // ThreadedKernel.alarm.waitUntil(500);

                        // Med is running
                        System.out.println("Med should be runing, lets check priorities");

                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        System.out.println("lowP releasing locks");
                        Machine.interrupt().disable();
                        // Release my locks
                        wakerupper.release();
                        contention.release();

                        // lowP is done
                        System.out.println("lowP is done");
                        KThread.finish();
                        Machine.interrupt().enable();
                    }
                });

                final KThread threadChecker = new KThread(new Runnable() {
                    public void run() {
                        wakerupper.acquire();
                        lowP.fork();
                        lowP.setName("Low");
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(lowP, 1);
                        Machine.interrupt().enable();

                        checkLowPriority.sleep();
                        Machine.interrupt().disable();
                        contention.getWaitQueue().print();
                        wakerupper.getWaitQueue().print();
                        System.out.println("Checking low is ready, high is blocked");
                        Assert.assertTrue(lowP.getStatus() == KThread.statusBlocked); // lowP
                                                                                      // is
                                                                                      // waiting
                                                                                      // for
                                                                                      // threadChecker
                        Assert.assertTrue(highP.getStatus() == KThread.statusBlocked);

                        Assert.assertEquals(((ThreadState) highP.schedulingState).getPriority(),
                                ((ThreadState) lowP.schedulingState).getEffectivePriority());
                        Assert.assertTrue(contention.getWaitQueue().contains(highP));
                        Assert.assertFalse(contention.getWaitQueue().contains(lowP));
                        Machine.interrupt().enable();

                        checkLowPriority.wake();
                        checkLowPriority.sleep();

                        System.out.println("Checking low is ready, high is blocked, med ready");
                        Assert.assertTrue(medP.getStatus() == KThread.statusReady);
                        Assert.assertTrue(lowP.getStatus() == KThread.statusBlocked); // Low
                                                                                      // waiting
                                                                                      // for
                                                                                      // us
                        Assert.assertTrue(highP.getStatus() == KThread.statusBlocked);

                        Assert.assertEquals(((ThreadState) highP.schedulingState).getPriority(),
                                ((ThreadState) lowP.schedulingState).getEffectivePriority());
                        Assert.assertTrue(contention.getWaitQueue().contains(highP));
                        Assert.assertFalse(contention.getWaitQueue().contains(lowP));

                        checkLowPriority.wake();
                        checkLowPriority.sleep();

                        System.out.println("Checking low is finished, high is blocked, med ready");
                        Assert.assertTrue(medP.getStatus() == KThread.statusReady);
                        Assert.assertTrue(lowP.getStatus() == KThread.statusFinished);
                        Assert.assertTrue(highP.getStatus() == KThread.statusBlocked); // High
                                                                                       // waiting
                                                                                       // for
                                                                                       // us

                        Assert.assertFalse(contention.getWaitQueue().contains(highP));
                        Assert.assertFalse(contention.getWaitQueue().contains(lowP));

                        checkLowPriority.wake();
                        wakerupper.release();
                        KThread.finish();
                    }
                });
                wakerupper.acquire();
                contention.acquire();
                threadChecker.fork();
                Machine.interrupt().disable();
                ThreadedKernel.scheduler.setPriority(threadChecker, 6);
                ThreadedKernel.scheduler.setPriority(7);
                Machine.interrupt().enable();

                contention.release();
                done.sleep();


                System.out.println("Checking all is finished");
                Assert.assertTrue(lowP.getStatus() == KThread.statusFinished);
                Assert.assertTrue(highP.getStatus() == KThread.statusFinished);
            }
        });
        
        enqueueJob(new Runnable() {
            public void run() {
                System.out.println("A -> B -> C");
                final Lock contention1 = new Lock();
                final Lock contention2 = new Lock();
                final Lock wakerupper = new Lock();
                final Condition checkLowPriority = new Condition(wakerupper);
                final Condition waitUntilMed = new Condition(wakerupper);
                final Condition done = new Condition(wakerupper);
                final Condition contentionAcquired = new Condition(wakerupper);
                final KThread highP = new KThread(new Runnable() {
                    public void run() {
                        System.out.println("High trying to acquire");
                        contention2.acquire();
                        
                        wakerupper.acquire();
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        done.wake();
                        wakerupper.release();
                        contention2.release();
                        KThread.finish();
                    }
                });
                final KThread medP = new KThread(new Runnable() {
                    public void run() {
                        System.out.println("Med trying to acquire");
                        contention2.acquire();
                        
                        contention1.acquire();
                        wakerupper.acquire();
                        
                        // lowP is done
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        Machine.interrupt().disable();
                      
                        wakerupper.release();
                        
                        contention1.release();
                        contention2.release();
                        KThread.finish();
                        Machine.interrupt().enable();
                    }
                });
                final KThread lowP = new KThread(new Runnable() {
                    public void run() {
                        System.out.println("Low trying to acquire");
                        wakerupper.acquire();
                        contention1.acquire();
                        
                        medP.fork();
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(medP, 4);
                        Machine.interrupt().enable();
                        
                        // Let medP go
                        while (contention1.getWaitQueue().empty()) {
                            KThread.yield();
                        }
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        highP.fork();
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(highP, 6);
                        Machine.interrupt().enable();
                        
                        while (contention2.getWaitQueue().empty()) {
                            KThread.yield();
                        }
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        Machine.interrupt().disable();
                        contention1.release();
                        wakerupper.release();
                        
                        KThread.finish();
                        Machine.interrupt().enable();
                    }
                });
                final KThread threadChecker = new KThread(new Runnable() {
                    public void run() {
                        System.out.println("Checker starting");
                        wakerupper.acquire();
                        lowP.fork();
                        Machine.interrupt().disable();
                        ThreadedKernel.scheduler.setPriority(lowP, 2);
                        Machine.interrupt().enable();
                        
                        checkLowPriority.sleep();
                        System.out.println("Med waiting for Low");
                        // Low should be blocked on us, Med should have contention 2
                        // and be waiting on contention 1
                        Machine.interrupt().disable();
                        Assert.assertEquals(ThreadedKernel.scheduler.getPriority(medP),
                                ThreadedKernel.scheduler.getEffectivePriority(lowP));
                        Machine.interrupt().enable();
                        Assert.assertTrue(contention1.getWaitQueue().contains(medP));
                        Assert.assertTrue(contention2.getLockHolder().equals(medP));
                        Assert.assertTrue(contention1.getLockHolder().equals(lowP));
                        Assert.assertEquals(KThread.statusBlocked, medP.getStatus());
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        // Now High should run and request contention2, the resource that
                        // medP holds
                        System.out.println("High waiting for Med waiting for Low");
                        
                        Machine.interrupt().disable();
                        Assert.assertEquals(ThreadedKernel.scheduler.getPriority(highP),
                                ThreadedKernel.scheduler.getEffectivePriority(lowP));
                        Assert.assertEquals(ThreadedKernel.scheduler.getPriority(highP),
                                ThreadedKernel.scheduler.getEffectivePriority(medP));
                        Machine.interrupt().enable();
                        Assert.assertTrue(contention1.getWaitQueue().contains(medP));
                        Assert.assertTrue(contention2.getWaitQueue().contains(highP));
                        Assert.assertTrue(contention2.getLockHolder().equals(medP));
                        Assert.assertTrue(contention1.getLockHolder().equals(lowP));
                        Assert.assertEquals(KThread.statusBlocked, medP.getStatus());
                        Assert.assertEquals(KThread.statusBlocked, highP.getStatus());
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        // Now lowP should be done, medP has contention1 and 2, high is blocked
                        System.out.println("High waiting for Med");
                        Machine.interrupt().disable();
                        Assert.assertEquals(ThreadedKernel.scheduler.getPriority(highP),
                                ThreadedKernel.scheduler.getEffectivePriority(medP));
                        Machine.interrupt().enable();
                        Assert.assertTrue(contention1.getWaitQueue().empty());
                        Assert.assertTrue(contention2.getWaitQueue().contains(highP));
                        Assert.assertTrue(contention2.getLockHolder().equals(medP));
                        Assert.assertTrue(contention1.getLockHolder().equals(medP));
                        Assert.assertEquals(KThread.statusBlocked, medP.getStatus());
                        Assert.assertEquals(KThread.statusBlocked, highP.getStatus());
                        Assert.assertEquals(KThread.statusFinished, lowP.getStatus());
                        
                        checkLowPriority.wake();
                        checkLowPriority.sleep();
                        
                        // LowP, MedP are done. High has contention 2
                        System.out.println("Low, Med done");
                        
                        Assert.assertTrue(contention1.getWaitQueue().empty());
                        Assert.assertTrue(contention2.getWaitQueue().empty());
                        Assert.assertTrue(contention2.getLockHolder().equals(highP));
                        Assert.assertTrue(contention1.getLockHolder() == null);
                        Assert.assertEquals(KThread.statusFinished, medP.getStatus());
                        Assert.assertEquals(KThread.statusBlocked, highP.getStatus());
                        Assert.assertEquals(KThread.statusFinished, lowP.getStatus());
                        
                        done.wake();
                        wakerupper.release();
                        System.out.println("Checker done");
                        KThread.finish();
                    }
                });
                wakerupper.acquire();
                threadChecker.fork();
                Machine.interrupt().disable();
                ThreadedKernel.scheduler.setPriority(threadChecker, 7);
                Machine.interrupt().enable();
                
                System.out.println("Sleep on done");
                done.sleep();
                System.out.println("All done");
                
                wakerupper.release();
                try {
                    Machine.halt();
                } catch (Exception e) {
                    Assert.fail();
                }

            }
        });

    }

}

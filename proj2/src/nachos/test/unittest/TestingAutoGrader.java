/**
 * 
 */
package nachos.test.unittest;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;

import nachos.ag.AutoGrader;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.security.Privilege;
import nachos.threads.Alarm;
import nachos.threads.Scheduler;

/**
 * This class overrides the behavior of the AutoGrader class to allow unit
 * testing within the nachos framework. Since nachos handles the set-up
 * internally it is very hard to break up it's logic and inject our own stubs
 * and/or code. Therefore this class does some not very kosher things to allow
 * much more fine-grained control of the Kernel.
 * 
 * @author Sasha
 * 
 */
public class TestingAutoGrader extends AutoGrader {

	private static Class<? extends Scheduler> schedulerClass;
	private static BlockingQueue<Runnable> instructionQueue;
	private static BlockingQueue<Object> messageQueue;

	/**
	 * Sets the {@link Scheduler} class used by the instance
	 * 
	 * @param scheduler
	 */
	public static void setScheduler(Class<? extends Scheduler> schedulerClass) {
		TestingAutoGrader.schedulerClass = schedulerClass;
	}

	public static void setInstructionQueue(
			BlockingQueue<Runnable> instructionQueue) {
		TestingAutoGrader.instructionQueue = instructionQueue;
	}

	/**
	 * Start this autograder. Extract the <tt>-#</tt> arguments, call
	 * <tt>init()</tt>, load and initialize the kernel, and call <tt>run()</tt>.
	 * 
	 * @param privilege
	 *            encapsulates privileged access to the Nachos machine.
	 */
	public void start(Privilege privilege) {
		Lib.assertTrue(this.privilege == null, "start() called multiple times");
		this.privilege = privilege;

		System.out.print(" grader");

		System.out.print("\n");

		try {
			kernel = new TestingThreadedKernel(schedulerClass.getConstructor(
					new Class[] {}).newInstance(new Object[] {}), new Alarm(),
					instructionQueue,messageQueue);

		} catch (InstantiationException e) {
			e.printStackTrace();
			Machine.terminate(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Machine.terminate(e);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Machine.terminate(e);
		} catch (SecurityException e) {
			e.printStackTrace();
			Machine.terminate(e);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Machine.terminate(e);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Machine.terminate(e);
		}
		kernel.initialize(null);

		init();

		run();
	}

	protected void init() {
		kernel.selfTest();
	}

	/**
	 * Runs the kernel does not call terminate, that's JUnit's problem
	 */
	protected void run() {
		kernel.run();

		// kernel.terminate();
	}

	public static void setInstructionQueue1(
			BlockingQueue<Runnable> instructionQueue) {
		TestingAutoGrader.instructionQueue = instructionQueue;
	}

	public static void setMessageQueue(BlockingQueue<Object> messageQueue) {
		TestingAutoGrader.messageQueue = messageQueue;

	}

}

package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.userprog.UserProcess.childProcess;

import java.util.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	static LinkedList<Integer> freePages;
	static ArrayList<Boolean> pageStatus;
	
	static {
		initPages();
	}
	
	public static void initPages() {
		freePages = new LinkedList<Integer>();
		pageStatus = new ArrayList<Boolean>();
		for (int i=0; i<Machine.processor().getNumPhysPages(); i++) {
			freePages.add(i);
			pageStatus.add(false);
		}
	}
	
	public static int allocatePage() {
		Machine.interrupt().disable();
		
		if (freePages.size() < 1) {
			Machine.interrupt().enable();
			return -1;
		} else {
			int selPage = freePages.pop();
			Lib.assertTrue(pageStatus.get(selPage) == false);
			pageStatus.set(selPage, true);
			Machine.interrupt().enable();
			return selPage;
		}
	}
	
	public static void deallocatePage(int selPage) {
		Machine.interrupt().disable();
		Lib.assertTrue(pageStatus.get(selPage) == true);
		pageStatus.set(selPage, false);
		freePages.push(selPage);
		Machine.interrupt().enable();
	}
	
	
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	/*System.out.println("Testing allocate");
	ArrayList<Integer> testAlloc = new ArrayList<Integer>();
	int allocPage;
	while ((allocPage = allocatePage()) >= 0) {
		testAlloc.add(allocPage);
	}
	int allocated = testAlloc.size();
	System.out.println(allocated + " pages allocated");
	
	System.out.println("Testing deallocate odd (fragmentation check)");
	int deallocated = 0;
	for (int i=testAlloc.size()-1; i>=0; i-=2) {
		deallocatePage( testAlloc.remove(i) );
		deallocated ++;
	}
	
	System.out.println("Reallocating odd pages");
	for (int i=0; i<deallocated; i++) {
		allocPage = allocatePage();
		Lib.assertTrue(allocPage >= 0);
		testAlloc.add(allocPage);
	}
	
	System.out.println("Deallocating all pages");
	deallocated = 0;
	for (Integer deallocPage:testAlloc) {
		deallocatePage(deallocPage);
		deallocated++;
	}
	testAlloc.clear();
	Lib.assertTrue(deallocated == allocated);
	
	System.out.println("Reallocating all pages");
	for (int i=0; i<allocated; i++) {
		testAlloc.add(allocatePage());
	}
	
	System.out.println("Deallocating all pages");
	for (Integer deallocPage:testAlloc) {
		deallocatePage(deallocPage);
	}
	
	System.out.println("Page allocation OK.");*/
	
	// ******************** CONSOLE TEST
	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}

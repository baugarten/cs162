package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
        private static final int ROOT = 1;
        private static int unique = ROOT;
        private int process_id;
        
        private UThread thread;
        private HashMap<Integer, childProcess> map;
        private childProcess myChildProcess;
        
        class childProcess{
                UserProcess child;
                int status;

                childProcess(UserProcess process){
                        this.child=process;
                        this.status=-999;
                }
        }
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        map=new HashMap<Integer, childProcess>();
        
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i=0; i<numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
        
        this.process_id = unique;
        unique++;
        
		openfiles = new HashMap<Integer, OpenFile>();
		available_descriptors = new ArrayList<Integer>(Arrays.asList(2,3,4,5,6,7,8,9,10,11,12,13,14,15));
		openfiles.put(0, UserKernel.console.openForReading());
		openfiles.put(1, UserKernel.console.openForWriting());
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;
        
        thread= new UThread(this);
        thread.setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        int transferred = 0;
        while (length > 0 && offset < data.length) {
        	int addrOffset = vaddr % 1024;
        	int virtualPage = vaddr / 1024;
        	
        	if (virtualPage >= pageTable.length || virtualPage < 0) {
        		break;
        	}
        	TranslationEntry pte = pageTable[virtualPage];
        	if (!pte.valid) {
        		break;
        	}
        	pte.used = true;
        	
        	int physPage = pte.ppn;
        	int physAddr = physPage * 1024 + addrOffset;
        	
        	int transferLength = Math.min(data.length-offset, Math.min(length, 1024-addrOffset));
        	System.arraycopy(memory, physAddr, data, offset, transferLength);
        	vaddr += transferLength;
        	offset += transferLength;
        	length -= transferLength;
        	transferred += transferLength;
        }
        
        return transferred;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        int transferred = 0;
        while (length > 0 && offset < data.length) {
        	int addrOffset = vaddr % 1024;
        	int virtualPage = vaddr / 1024;
        	
        	if (virtualPage >= pageTable.length || virtualPage < 0) {
        		break;
        	}
        	
        	TranslationEntry pte = pageTable[virtualPage];
        	if (!pte.valid || pte.readOnly) {
        		break;
        	}
        	pte.used = true;
        	pte.dirty = true;
        	
        	int physPage = pte.ppn;
        	int physAddr = physPage * 1024 + addrOffset;
        	
        	int transferLength = Math.min(data.length-offset, Math.min(length, 1024-addrOffset));
        	System.arraycopy(data, offset, memory, physAddr, transferLength);
        	vaddr += transferLength;
        	offset += transferLength;
        	length -= transferLength;
        	transferred += transferLength;
        }
        
        return transferred;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();	

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                       argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }
        pageTable = new TranslationEntry[numPages];
        
        for (int i=0; i<numPages; i++) {
        	int physPage = UserKernel.allocatePage();
        	if (physPage < 0) {
        		Lib.debug(dbgProcess, "\tunable to allocate pages; tried " + numPages + ", did " + i );
        		for (int j=0; j<i; j++) {
        			if (pageTable[j].valid) {
        				UserKernel.deallocatePage(pageTable[j].ppn);
        				pageTable[j].valid = false;
        			}
        		}
        		coff.close();
        		return false;
        	}
        	pageTable[i] = new TranslationEntry(
        			i, physPage, true, false, false, false);
        }
        
        // load sections
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                      + " section (" + section.getLength() + " pages)");

            for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;

                // for now, just assume virtual addresses=physical addresses
                int ppn = pageTable[vpn].ppn;
                section.loadPage(i, ppn);
                if (section.isReadOnly()) {
                	pageTable[vpn].readOnly = true;
                }
            }
        }
        
        coff.close();
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	for (int i=0; i<pageTable.length; i++) {
    		if (pageTable[i].valid) {
    			UserKernel.deallocatePage(pageTable[i].ppn);
    			pageTable[i].valid = false;
    		}
    	}
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    if (this.process_id != ROOT) {
    	return 0;
    }
    Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }


    private static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
    case syscallExit:
            handleExit(a0);
    case syscallExec:
            return handleExec(a0,a1,a2);
    case syscallJoin: 
            return handleJoin(a0,a1);
    default:
        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
        Lib.assertNotReached("Unknown system call!");
    }
    return 0;
    }


        /*
         * if the input arguments are negative, return -1
         * @return the child process Id 
         */
        private int handleExec(int file, int argc, int argv){
                if(file<0 || argc<0 || argv<0){
                        return -1;
                }
                String fileName= readVirtualMemoryString(file,256);

                if(fileName==null){
                        return -1;
                }

                String args[]= new String[argc];

                int byteReceived,argAddress;
                byte temp[]=new byte[4];
                for(int i =0; i<argc; i++){
                        byteReceived=readVirtualMemory(argv+i*4,temp);
                        if(byteReceived !=4){
                                return -1;
                        }

                        argAddress=Lib.bytesToInt(temp, 0);
                        args[i]=readVirtualMemoryString(argAddress, 256);

                        if(args[i]==null){
                                return -1;
                        }

                }
                UserProcess child=UserProcess.newUserProcess();
        		childProcess newProcessData = new childProcess(child);
                child.myChildProcess = newProcessData;
                
                if(child.execute(fileName, args)){
                    map.put(child.process_id, newProcessData);
                    return child.process_id;
                }

                return -1;
        }


        /*
         * pid  is the child process going to join 
         * status is the vritual address that we need to write the result on it.
         */

        private int handleJoin(int pid,int status){
                if (pid <0 || status<0){
                        return -1;
                }
                //get the child process from our hashmap
                childProcess childData;
                if(map.containsKey(pid)){
                	childData = map.get(pid);
                }
                else{
                        return -1;
                }

                //join it
                childData.child.thread.join();
                
                //remove from hashmap
                map.remove(pid);

                //write the exit # to the address status
                if(childData.status!=-999){
                        byte exitStatus[] = new byte[4];
                        exitStatus=Lib.bytesFromInt(childData.status);
                        int byteTransfered=writeVirtualMemory(status,exitStatus);

                        if(byteTransfered == 4){
                                return 1;
                        }
                        else{
                                return 0;
                        }

                }
                return 0;
        }
        
        private void handleExit(int status){
                if(myChildProcess!=null){
                	myChildProcess.status = status;
                }
                
                //close all the opened files
                for (int i=0; i<16; i++) {              
                        handleClose(i);
                }
                
                //part2 implemented
                this.unloadSections();
                
                if(this.process_id==ROOT){
                        Kernel.kernel.terminate();
                }
                
                else{
                        KThread.finish();
                    	Lib.assertNotReached();
                }
        }

    private int handleUnlink(int a0) {
    	String filename = this.readVirtualMemoryString(a0, 255);
		if (filename == null) {
			return -1;
		}
		return ThreadedKernel.fileSystem.remove(filename) ? 0 : -1;
	}

	private int handleClose(int a0) {
		OpenFile file = openfiles.get(a0);
		if (file == null) return -1;
		file.close();
		openfiles.remove(a0);
		available_descriptors.add(a0);
		return 0;
	}

	private int handleWrite(int a0, int bufaddr, int count) {
		OpenFile file = openfiles.get(a0);
		if (file == null) return -1;
		byte[] transfer_buffer = new byte[Processor.pageSize];
		int total_transfer = 0;
		while (count > 0) {
			int readlen = Math.min(Processor.pageSize, count);
			
			int actualread = readVirtualMemory(bufaddr, transfer_buffer, 0, readlen);
			if (actualread == -1) return -1;
			if (actualread < readlen) return -1;
			
			int written_bytes = file.write(transfer_buffer, 0, actualread);
			if (written_bytes != actualread) {
				return -1;
			}
			count -= actualread;
			bufaddr += actualread;
			total_transfer += actualread;
		}
		return total_transfer;
	}

	private int handleRead(int a0, int bufaddr, int count) {
		OpenFile file = openfiles.get(a0);
		if (file == null) return -1;
		byte[] transfer_buffer = new byte[Processor.pageSize];
		boolean done = false;
		int total_transfer = 0;
		while (!done && count > 0) {
			int readlen = Math.min(Processor.pageSize, count);
			
			int actualread = file.read(transfer_buffer, 0, readlen);
			if (actualread == -1) return -1;
			if (actualread < readlen) done = true;
			
			int written_bytes = writeVirtualMemory(bufaddr, transfer_buffer, 0, actualread);
			if (written_bytes != actualread) {
				return -1;
			}
			count -= actualread;
			bufaddr += actualread;
			total_transfer += actualread;
		}
		return total_transfer;
	}

	private int handleOpen(int a0) {
		String filename = this.readVirtualMemoryString(a0, 255);
		if (filename == null) {
			return -1;
		}
		OpenFile file = ThreadedKernel.fileSystem.open(filename, false);
		if (file == null) {
			return -1;
		}
		if (available_descriptors.size() < 1) {
			return -1;
		}
		Integer filedescriptor = available_descriptors.remove(0);
		openfiles.put(filedescriptor, file);
		return filedescriptor;
	}

	private int handleCreate(int a0) {
		String filename = this.readVirtualMemoryString(a0, 255);
		if (filename == null) {
			return -1;
		}
		OpenFile file = ThreadedKernel.fileSystem.open(filename, true);
		if (file == null) {
			return -1;
		}
		if (available_descriptors.size() < 1) {
			return -1;
		}
		Integer filedescriptor = available_descriptors.remove(0);
		openfiles.put(filedescriptor, file);
		return filedescriptor;
	}

	/**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
        case Processor.exceptionSyscall:
            int result = handleSyscall(processor.readRegister(Processor.regV0),
                                       processor.readRegister(Processor.regA0),
                                       processor.readRegister(Processor.regA1),
                                       processor.readRegister(Processor.regA2),
                                       processor.readRegister(Processor.regA3)
                                       );
            processor.writeRegister(Processor.regV0, result);
            processor.advancePC();
            break;				       
                                       
        default:
            Lib.debug(dbgProcess, "Unexpected exception: " +
                      Processor.exceptionNames[cause]);
            handleExit(-1);
            Lib.assertNotReached("Unexpected exception");
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;

    private HashMap<Integer, OpenFile> openfiles;
    private List<Integer> available_descriptors;
    
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}

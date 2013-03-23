/* execersub.c 
 *    This program sets a long array, then scans through it and returns
 *    the entry of they are all the same, or -1 if there are any differences
 *    Designed to test proper application separation
 */

int
main(int argc, char *argv[])
{
    char *progArgs[1];
    char progStr[2];
    int pid, rtn, status;
    
    progArgs[0] = progStr;
    progStr[0] = 56;
    progStr[1] = 0;

    printf("\nStarting process\n");
	pid = exec("p3-exit_sub.coff", 1, progArgs);
	
    rtn = join(pid, &status);
    printf("\n[%d] Join = %d; returned %d\n", pid, rtn, status);
    

    exit(0);
}

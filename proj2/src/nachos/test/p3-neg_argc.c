/* execer.c 
 *	Test program to do matrix multiplication on large arrays.
 *
 *	Intended to stress virtual memory system. Should return 7220 if Dim==20
 */

#include "stdlib.h"
#include "stdio.h"
#include "stdlib.h"

int main()
{
    char *progArgs[1];
    char progStr[2];
	char *prog = "execersub.coff";
    int pid;
    int status, rtn;
	
    progArgs[0] = progStr;
    progStr[1] = 0;

	progStr[0] = 2;
	printf("\nTest negative argc\n", pid);
	pid = exec(prog, -1, progArgs);
	printf("\nPID = %d\n", pid);    
	rtn = join(pid, &status);
    printf("\nJoin = %d; returned status = %d\n", rtn, status);

	progArgs[0] = 0;
	printf("\nTest zero argv\n", pid);
	pid = exec(prog, -1, progArgs);
	printf("\nPID = %d\n", pid);    
	rtn = join(pid, &status);
    printf("\nJoin = %d; returned status = %d\n", rtn, status);

	progArgs[0] = -128;
	printf("\nTest negative argv\n", pid);
	pid = exec(prog, -1, progArgs);
	printf("\nPID = %d\n", pid);    
	rtn = join(pid, &status);
    printf("\nJoin = %d; returned status = %d\n", rtn, status);
    
    printf("\nDone\n");
    
    return 1;
}
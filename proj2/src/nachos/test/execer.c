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
    int pid1, pid2, pid3, pid4, pid5;
    int status;
	
    progArgs[0] = progStr;
    
    progStr[1] = 0;

	progStr[0] = 2;
	pid1 = exec(prog, 1, progArgs);
	
	progStr[0] = 5;
	pid2 = exec(prog, 1, progArgs);

	progStr[0] = 13;
	pid3 = exec(prog, 1, progArgs);

	join(pid1, &status);
    printf("\n[%d] %d returnd %d\n", pid1, 1, status);

	join(pid2, &status);
    printf("\n[%d] %d returnd %d\n", pid2, 2, status);
    
	join(pid3, &status);
    printf("\n[%d] %d returnd %d\n", pid3, 3, status);
    
    printf("\nDone\n");
    
    return 1;
}
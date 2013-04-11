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
    int pid1;
    int rtn, status;
	
    progArgs[0] = progStr;
    
    progStr[1] = 0;

    printf("\nStarting process\n", pid1, 1, status);
    
	progStr[0] = 2;
	pid1 = exec(prog, 1, progArgs);
	
	rtn = join(pid1, &status);
    printf("\n[%d] Join = %d; returned status = %d\n", pid1, rtn, status);

	rtn = join(pid1, &status);
    printf("\n[%d] Join = %d; returned status = %d\n", pid1, rtn, status);

	rtn = join(pid1, &status);
    printf("\n[%d] Join = %d; returned status = %d\n", pid1, rtn, status);
    
    printf("\nDone\n");
    
    return 1;
}
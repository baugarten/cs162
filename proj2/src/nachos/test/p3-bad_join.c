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
    int status, rtn;

	rtn = join(0, &status);
    printf("\n[0] Join = %d; returned status = %d\n", rtn, status);

	rtn = join(-99, &status);
    printf("\n[-99] Join = %d; returned status = %d\n", rtn, status);
    
	rtn = join(99, &status);
    printf("\n[99] Join = %d; returned status = %d\n", rtn, status);
    
    printf("\nDone\n");
    
    return 1;
}
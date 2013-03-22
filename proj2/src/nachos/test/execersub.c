/* execersub.c 
 *    This program sets a long array, then scans through it and returns
 *    the entry of they are all the same, or -1 if there are any differences
 *    Designed to test proper application separation
 */
#define Dim 	4097	/* sum total of the arrays doesn't fit in 
			 * physical memory 
			 */
             
int
main(int argc, char *argv[])
{
    char crazy[Dim];
    int paramIn, guard1, guard2, i;
    
    if (argc != 1) {
        return -1;
    }
    
    paramIn = *(argv[0]);
    
    guard1 = (paramIn + 4239) % 123;
    guard2 = (paramIn + 849) % 89;
    
    i=0;
    
    crazy[0] = guard1;
    crazy[Dim-1] = guard2;
    
    for (i=1; i<Dim-1; i++) {
        crazy[i] = paramIn;
    }
    
    for (i=1; i<Dim-2; i++) {
        if (crazy[i] != paramIn) {
            return -1;
        }
    }
    if (crazy[0] != (paramIn + 4239) % 123
    || crazy[Dim-1] != (paramIn + 849) % 89) {
        return -1;
    }

    return paramIn;
}

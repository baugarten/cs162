/* execersub.c 
 *    This program sets a long array, then scans through it and returns
 *    the entry of they are all the same, or -1 if there are any differences
 *    Designed to test proper application separation
 */

int
main(int argc, char *argv[])
{
    int paramIn;
    
    if (argc != 1) {
        return -1;
    }
    
    paramIn = *(argv[0]);
    
    exit( paramIn );
}

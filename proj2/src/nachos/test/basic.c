#include "stdio.h"
#include "syscall.h"

int main(int argc, char* argv) {
	char* filename = "tmp.txt";
	int descriptor = creat(filename);
	int again;
	int amt_written, closed, unlinked;
	char buf[50];
	char empty[50];
	int amount_read = read(descriptor, buf, 20);
	printf("%d should be %d\n", amount_read, 0);
	strcpy(buf,"C programming is not for everyone");
	

	amt_written = write(descriptor, buf, 32);
	printf("%d should be %d\n", amt_written, 32);
	again = open(filename);
	
	amount_read = read(again, empty, 32);
	printf("%d should be %d\n", amount_read, 32);
	closed = close(descriptor);
	printf("%d should be %d\n", closed, 0);

	amount_read = read(descriptor, buf, 32);
	printf("%d should be %d\n", amount_read, -1);
	amt_written = write(descriptor, buf, 32);
	printf("%d should be %d\n", amt_written, -1);

	unlinked = unlink("tmp.txt");
	printf("%d should be %d\n", unlinked, 0);

	descriptor = open("tmp.txt");
	printf("%d should be %d\n", descriptor, -1);
	printf("%s\n", buf);
}

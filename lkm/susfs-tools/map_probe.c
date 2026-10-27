#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main(int argc, char **argv)
{
	int fd, found = 0;
	void *p;
	FILE *f;
	char line[1024];

	if (argc != 2)
		return 2;
	fd = open(argv[1], O_RDONLY);
	if (fd < 0)
		return 3;
	p = mmap(NULL, 4096, PROT_READ, MAP_PRIVATE, fd, 0);
	if (p == MAP_FAILED)
		return 4;
	f = fopen("/proc/self/maps", "r");
	if (!f)
		return 5;
	while (fgets(line, sizeof(line), f)) {
		if (strstr(line, argv[1])) {
			printf("MAP %s", line);
			found = 1;
		}
	}
	fclose(f);
	munmap(p, 4096);
	close(fd);
	return found ? 0 : 1;
}

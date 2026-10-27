#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

static int scan(const char *path, const char *needle, const char *tag)
{
	FILE *f = fopen(path, "r");
	char line[1024];
	int found = 0;

	if (!f)
		return -1;
	while (fgets(line, sizeof(line), f)) {
		if (strstr(line, needle)) {
			printf("%s %s", tag, line);
			found = 1;
		}
	}
	fclose(f);
	return found;
}

int main(int argc, char **argv)
{
	int fd, m, s;
	void *p;

	if (argc != 2)
		return 2;
	fd = open(argv[1], O_RDONLY);
	if (fd < 0)
		return 3;
	p = mmap(NULL, 4096, PROT_READ, MAP_PRIVATE, fd, 0);
	if (p == MAP_FAILED)
		return 4;
	m = scan("/proc/self/maps", argv[1], "MAP");
	s = scan("/proc/self/smaps", argv[1], "SMAP");
	munmap(p, 4096);
	close(fd);
	return (m > 0 || s > 0) ? 0 : 1;
}

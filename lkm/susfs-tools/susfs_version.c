#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

struct st_susfs_version {
	char susfs_version[16];
	int err;
};

int main(void)
{
	struct st_susfs_version info;
	long rc;

	memset(&info, 0, sizeof(info));
	errno = 0;
	rc = syscall(SYS_reboot, 0xDEADBEEF, 0xFAFAFAFA, 0x555e1, &info);
	printf("rc=%ld errno=%d version=%s err=%d\n",
	       rc, errno, info.susfs_version, info.err);
	return 0;
}

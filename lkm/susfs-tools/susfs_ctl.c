#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

#define CMD_SET_UNAME 0x55590
#define CMD_HIDE_MOUNTS 0x55561
#define CMD_ADD_SUS_MAP 0x60020

struct st_uname {
	char release[65];
	char version[65];
	char pad[2];
	int err;
};

struct st_hide_mounts {
	unsigned char enabled;
	unsigned char pad[3];
	int err;
};

struct st_sus_map {
	char path[256];
	int err;
};

static long susfs_call(unsigned cmd, void *payload)
{
	return syscall(SYS_reboot, 0xDEADBEEF, 0xFAFAFAFA, cmd, payload);
}

int main(int argc, char **argv)
{
	if (argc < 2) {
		fprintf(stderr, "usage: %s uname <release> <version> | hide_mounts 0|1 | map <path>\n", argv[0]);
		return 2;
	}
	if (!strcmp(argv[1], "uname") && argc == 4) {
		struct st_uname u;
		long rc;
		memset(&u, 0, sizeof(u));
		strncpy(u.release, argv[2], sizeof(u.release) - 1);
		strncpy(u.version, argv[3], sizeof(u.version) - 1);
		u.err = 126;
		rc = susfs_call(CMD_SET_UNAME, &u);
		printf("rc=%ld errno=%d err=%d\n", rc, errno, u.err);
		return rc == 0 && u.err == 0 ? 0 : 1;
	}
	if (!strcmp(argv[1], "hide_mounts") && argc == 3) {
		struct st_hide_mounts h;
		long rc;
		memset(&h, 0, sizeof(h));
		h.enabled = (unsigned char)atoi(argv[2]);
		h.err = 126;
		rc = susfs_call(CMD_HIDE_MOUNTS, &h);
		printf("rc=%ld errno=%d err=%d\n", rc, errno, h.err);
		return rc == 0 && h.err == 0 ? 0 : 1;
	}
	if (!strcmp(argv[1], "map") && argc == 3) {
		struct st_sus_map m;
		long rc;
		memset(&m, 0, sizeof(m));
		strncpy(m.path, argv[2], sizeof(m.path) - 1);
		m.err = 126;
		rc = susfs_call(CMD_ADD_SUS_MAP, &m);
		printf("rc=%ld errno=%d err=%d\n", rc, errno, m.err);
		return rc == 0 && m.err == 0 ? 0 : 1;
	}
	fprintf(stderr, "bad arguments\n");
	return 2;
}

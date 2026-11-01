#!/bin/bash
# Build the Hybrid Mount VFS provider LKM for GKI 6.18.21.
# Run inside ghcr.io/ylarod/ddk-min:android17-6.18-20260828 with the
# vendored source, KERNEL_SRC and KDIR mounted; see lkm/README.md.
set -e
HM=${HM:-/work/hybridmount}
KDIR=${KDIR:-/work/kdir/android17-6.18}
OUT=${OUT:-/out/hybridmount-6.18.21.ko}
export ARCH=arm64

make -k -C "$KDIR" M="$HM" ARCH=arm64 CC=clang LLVM=1 LLVM_IAS=1 \
	modules -j"$(nproc)"

cp "$HM/hybridmount.ko" "$OUT"
ls -l "$OUT"
strings -a "$OUT" | grep -m1 'vermagic='
echo BUILD_HYBRIDMOUNT_DONE

#!/bin/bash
set -e
export ARCH=arm64 CROSS_COMPILE=aarch64-linux-gnu-
KDIR=/work/kdir/android17-6.18
cd /work/sukisu/kernel
make -C "$KDIR" M="$PWD" MO="$PWD" CONFIG_KSU=m CC=clang modules -j"$(nproc)" > /out/sukisu-build.log 2>&1 || { tail -30 /out/sukisu-build.log; exit 1; }
tail -5 /out/sukisu-build.log
cp kernelsu.ko /out/sukisu-kernelsu.ko
llvm-strip -d /out/sukisu-kernelsu.ko || true
strings -a /out/sukisu-kernelsu.ko | grep -m1 'vermagic='
ls -l /out/sukisu-kernelsu.ko

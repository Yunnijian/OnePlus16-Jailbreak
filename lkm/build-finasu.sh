#!/bin/bash
set -e
export ARCH=arm64 CROSS_COMPILE=aarch64-linux-gnu-
KDIR=/work/kdir/android17-6.18
cd /work/finasu/kernel
make -C "$KDIR" M="$PWD" MO="$PWD" CONFIG_KSU=m CC=clang modules -j"$(nproc)" > /out/finasu-build.log 2>&1 || { tail -30 /out/finasu-build.log; exit 1; }
tail -5 /out/finasu-build.log
cp kernelsu.ko /out/finasu-kernelsu.ko
llvm-strip -d /out/finasu-kernelsu.ko || true
strings -a /out/finasu-kernelsu.ko | grep -m1 'vermagic='
ls -l /out/finasu-kernelsu.ko

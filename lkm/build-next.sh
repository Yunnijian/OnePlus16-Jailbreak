#!/bin/bash
# Build KernelSU Next LKM for GKI 6.18.21.
# Run inside: ghcr.io/ylarod/ddk-min:android17-6.18-20260828
# Mount the Next source at /work/ksun and a prepared KDIR at /work/kdir.
set -e
SRC=${SRC:-/work/ksun/kernel}
KDIR=${KDIR:-/work/kdir/android17-6.18}
OUT=${OUT:-/out/next-kernelsu.ko}
export ARCH=arm64 CROSS_COMPILE=aarch64-linux-gnu-
cd "$SRC"
make -C "$KDIR" M="$PWD" MO="$PWD" CONFIG_KSU=m CC=clang modules -j"$(nproc)"
cp kernelsu.ko "$OUT"
llvm-strip -d "$OUT" || true
strings -a "$OUT" | grep -m1 'vermagic='

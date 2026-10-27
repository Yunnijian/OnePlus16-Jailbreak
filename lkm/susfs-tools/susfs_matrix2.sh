#!/system/bin/sh
# SUSFS LKM rule matrix, corrected UIDs:
#  - sus_path / sus_kstat: app uid 10000 (su 10000 keeps the app uid)
#  - open_redirect: scheme 0 + uid 2000 (non-app, non-su gate is UID-based)
T=/data/local/tmp/susfs-test
CTL=/data/local/tmp/susfs_ctl
P=0; F=0; S=0; N=0
ok()  { echo "PASS  $*"; P=$((P+1)); }
bad() { echo "FAIL  $*"; F=$((F+1)); }
skip(){ echo "SKIP  $*"; S=$((S+1)); }
note(){ echo "NOTE  $*"; N=$((N+1)); }
r2000(){ su 2000 -c "$1"; }
r10000(){ su 10000 -c "$1"; }
rroot(){ su -c "$1"; }

rm -rf "$T"; mkdir -p "$T"
echo secret  > "$T/secret"
echo kfile   > "$T/kfile"
echo TARGET  > "$T/target"
echo BACKING > "$T/backing"
chmod 755 "$T"; chmod 644 "$T"/*

echo "=== 1. sus_path (app uid 10000) ==="
r10000 "cat $T/secret" | grep -qx secret && ok "baseline app read" || bad "baseline app read"
echo "add $T/secret" > /proc/susfs_path
r10000 "stat -c %i $T/secret" >/dev/null 2>&1 && bad "app stat still visible" || ok "app stat ENOENT"
r10000 "ls $T" | grep -qx secret && bad "app readdir still lists" || ok "app readdir hides"
rroot "stat -c %i $T/secret" >/dev/null 2>&1 && ok "root still sees" || bad "root lost path"
echo "del $T/secret" > /proc/susfs_path
r10000 "stat -c %i $T/secret" >/dev/null 2>&1 && ok "sus_path del restores" || bad "sus_path del failed"

echo "=== 2. sus_kstat (app uid 10000) ==="
echo "add_sus_kstat_statically $T/kfile 4242 777 1 4096 100 0 200 0 300 0 8 4096" > /proc/susfs_kstat
out=$(r10000 "stat -c '%i %s %Y' $T/kfile" 2>/dev/null)
[ "$out" = "4242 4096 200" ] && ok "kstat spoof ($out)" || bad "kstat got '$out'"
echo "del $T/kfile" > /proc/susfs_kstat
out=$(r10000 "stat -c '%i %s %Y' $T/kfile" 2>/dev/null)
[ "$out" != "4242 4096 200" ] && ok "kstat del restores" || bad "kstat still spoofed"

echo "=== 3. open_redirect (scheme 0, uid 2000) ==="
echo "add_open_redirect $T/target $T/backing 0" > /proc/susfs_open_redirect
out=$(r2000 "cat $T/target" 2>/dev/null)
[ "$out" = BACKING ] && ok "non-su redirected" || bad "non-su got '$out'"
out=$(rroot "cat $T/target" 2>/dev/null)
[ "$out" = BACKING ] && ok "root also redirected (scheme 0 covers non-app)" || bad "root got '$out'"
# scheme 2: 非 su 重定向、root 不受影响；需要直接 adb shell 的非 su 进程，脚本内 su 仍属 su 域，故不在此测
echo "del $T/target" > /proc/susfs_open_redirect
out=$(r2000 "cat $T/target" 2>/dev/null)
[ "$out" = TARGET ] && ok "redirect del restores" || bad "redirect still active"

echo "=== 4. hide_modules ==="
echo "add kernelsu" > /proc/susfs_hide_modules
c=$(r2000 "grep -c kernelsu /proc/modules" 2>/dev/null)
[ "$c" = "0" ] && ok "modules hidden for uid2000" || bad "uid2000 sees kernelsu=$c"
c=$(rroot "grep -c kernelsu /proc/modules" 2>/dev/null)
[ "$c" = "0" ] && ok "modules hidden for root" || bad "root sees kernelsu=$c"
r2000 "ls -d /sys/module/kernelsu" >/dev/null 2>&1 && bad "sysfs visible for uid2000" || ok "sysfs hidden for uid2000"
if rroot "ls -d /sys/module/kernelsu" >/dev/null 2>&1; then
	ok "sysfs visible for root"
else
	note "sysfs hidden for root too (u:r:ksu:s0 not recognised as su_ctx)"
fi
c=$(rroot "grep -c '\[kernelsu\]' /proc/kallsyms" 2>/dev/null)
[ "$c" = "0" ] && ok "kallsyms module lines hidden" || bad "kallsyms kernelsu lines=$c"
echo "del kernelsu" > /proc/susfs_hide_modules
c=$(rroot "grep -c kernelsu /proc/modules" 2>/dev/null)
[ "$c" != "0" ] && ok "hide_modules del restores" || bad "kernelsu still hidden"

echo "=== 5. enable_log / avc_spoof ==="
echo 0 > /proc/susfs_enable_log 2>/dev/null
el0=$(cat /proc/susfs_enable_log 2>/dev/null | head -1)
echo 1 > /proc/susfs_enable_log 2>/dev/null
el1=$(cat /proc/susfs_enable_log 2>/dev/null | head -1)
[ -n "$el0" ] && [ -n "$el1" ] && ok "enable_log toggle" || bad "enable_log toggle"
echo 1 > /proc/susfs_avc_spoof 2>/dev/null
av1=$(cat /proc/susfs_avc_spoof 2>/dev/null | head -1)
echo 0 > /proc/susfs_avc_spoof 2>/dev/null
av0=$(cat /proc/susfs_avc_spoof 2>/dev/null | head -1)
[ -n "$av1" ] && [ -n "$av0" ] && ok "avc_spoof toggle" || bad "avc_spoof toggle"

echo "=== 6. opath diagnostics ==="
n=$(cat /proc/susfs_opath 2>/dev/null | grep -c 'installed=1')
[ "$n" -ge 5 ] && ok "opath entries installed=$n" || bad "opath entries installed=$n"

echo "=== 7. uname spoof / restore ==="
real_r=$(rroot "uname -r" 2>/dev/null); real_v=$(rroot "uname -v" 2>/dev/null)
$CTL uname 6.18.21-susfs-test matrix >/dev/null 2>&1
[ "$(r10000 "uname -r" 2>/dev/null)" = 6.18.21-susfs-test ] && ok "uname spoof" || bad "uname spoof"
$CTL uname "$real_r" "$real_v" >/dev/null 2>&1
[ "$(r10000 "uname -r" 2>/dev/null)" = "$real_r" ] && ok "uname restored" || bad "uname restore"

echo "=== 8. hide_mounts prefix readback ==="
echo "add $T/mnt/" > /proc/susfs_hide_mounts 2>/dev/null
cat /proc/susfs_hide_mounts 2>/dev/null | grep -q "$T/mnt/" && ok "hide_mounts prefix add" || bad "hide_mounts prefix add"
echo "del $T/mnt/" > /proc/susfs_hide_mounts 2>/dev/null
cat /proc/susfs_hide_mounts 2>/dev/null | grep -q "$T/mnt/" && bad "hide_mounts prefix del" || ok "hide_mounts prefix del"

echo "=== 9. sus_map (app uid 10000) ==="
if [ ! -x /data/local/tmp/map_probe ]; then
	skip "sus_map: /data/local/tmp/map_probe missing"
else
	dd if=/dev/zero of="$T/mapped" bs=4096 count=1 2>/dev/null
	chmod 644 "$T/mapped"
	out=$(r10000 "/data/local/tmp/map_probe $T/mapped" 2>/dev/null)
	[ -n "$out" ] && ok "map visible before rule" || bad "map not visible before rule"
	$CTL map "$T/mapped" >/dev/null 2>&1
	out=$(r10000 "/data/local/tmp/map_probe $T/mapped" 2>/dev/null)
	[ -z "$out" ] && ok "sus_map maps/smaps hidden" || bad "sus_map still visible: $out"
	note "sus_map rule is add-only; reload the module to clear it"
fi

echo "=== 10. advanced (not exercised) ==="
skip "spoof_cmdline: persistent /proc/cmdline spoof; restore requires module reload"
skip "sus_mount actual hide: bind mount under /data/local/tmp is not marked (rescans=0 recorded=0)"

echo "=== cleanup ==="
echo clear > /proc/susfs_kstat 2>/dev/null
echo clear > /proc/susfs_open_redirect 2>/dev/null
echo "del $T/secret" > /proc/susfs_path 2>/dev/null
rm -rf "$T"
echo "=== RESULT pass=$P fail=$F skip=$S note=$N ==="

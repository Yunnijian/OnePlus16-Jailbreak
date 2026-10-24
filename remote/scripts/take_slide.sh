#!/system/bin/sh
D=${PYB110_DIR:-/data/local/tmp}
rm -f $D/bl_*.bin $D/bl_text.txt
echo 1 > /sys/kernel/tracing/events/sched/sched_blocked_reason/enable 2>/dev/null
echo 1 > /sys/kernel/tracing/tracing_on 2>/dev/null
for c in 0 1 2 3 4 5 6 7; do
  cat /sys/kernel/tracing/per_cpu/cpu$c/trace_pipe_raw > $D/bl_$c.bin 2>/dev/null &
done
sleep 1
i=0
while [ $i -lt 300 ]; do
  $D/forker 2 30 >/dev/null 2>&1
  i=$((i+1))
done
grep "sched_blocked_reason" /sys/kernel/tracing/trace > $D/bl_text.txt 2>/dev/null
pkill -f trace_pipe_raw 2>/dev/null
sleep 1
ls -l $D/bl_*.bin | awk '{print $5, $9}'
wc -l < $D/bl_text.txt

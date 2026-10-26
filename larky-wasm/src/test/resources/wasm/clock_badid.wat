;; Calls clock_time_get with clock ids 4 and -1, which WASI does not define, and exits with
;; errno(4) + 100 * errno(-1): 2828 when both are EINVAL (28). Exits with 99 if either call wrote
;; a timestamp.
(module
  (import "wasi_snapshot_preview1" "clock_time_get" (func $clock_time_get (param i32 i64 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory (export "memory") 1)
  (func $errno (param $id i32) (result i32)
    (local $errno i32)
    (i64.store (i32.const 512) (i64.const 12345))
    (local.set $errno (call $clock_time_get (local.get $id) (i64.const 1) (i32.const 512)))
    (if (i64.ne (i64.load (i32.const 512)) (i64.const 12345))
      (then (call $proc_exit (i32.const 99))))
    (local.get $errno))
  (func (export "_start")
    (call $proc_exit
      (i32.add
        (call $errno (i32.const 4))
        (i32.mul (call $errno (i32.const -1)) (i32.const 100))))))

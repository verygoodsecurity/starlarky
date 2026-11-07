;; Calls WASI functions outside the allowed set and writes each errno to stdout, space-separated:
;; poll_oneoff (a 5 s clock subscription), clock_res_get, path_open, fd_renumber, proc_raise.
;; Each must return NOSYS (52) at once.
(module
  (import "wasi_snapshot_preview1" "poll_oneoff" (func $poll_oneoff (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "clock_res_get" (func $clock_res_get (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "path_open" (func $path_open (param i32 i32 i32 i32 i32 i64 i64 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_renumber" (func $fd_renumber (param i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_raise" (func $proc_raise (param i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  ;; Writes $v in decimal and a space to stdout, using 3000..3032 and an iovec at 3100.
  (func $put (param $v i32)
    (local $p i32)
    (local.set $p (i32.const 3031))
    (i32.store8 (i32.const 3031) (i32.const 32))
    (loop $digit
      (local.set $p (i32.sub (local.get $p) (i32.const 1)))
      (i32.store8 (local.get $p) (i32.add (i32.const 48) (i32.rem_u (local.get $v) (i32.const 10))))
      (local.set $v (i32.div_u (local.get $v) (i32.const 10)))
      (br_if $digit (i32.ne (local.get $v) (i32.const 0))))
    (i32.store (i32.const 3100) (local.get $p))
    (i32.store (i32.const 3104) (i32.sub (i32.const 3032) (local.get $p)))
    (drop (call $fd_write (i32.const 1) (i32.const 3100) (i32.const 1) (i32.const 3108))))
  (func (export "_start")
    ;; One subscription at 0: userdata 0, tag 0 (clock), clock id 0, timeout 5e9 ns, precision 0,
    ;; flags 0 (relative).
    (i64.store (i32.const 0) (i64.const 0))
    (i32.store8 (i32.const 8) (i32.const 0))
    (i32.store (i32.const 16) (i32.const 0))
    (i64.store (i32.const 24) (i64.const 5000000000))
    (i64.store (i32.const 32) (i64.const 0))
    (i32.store16 (i32.const 40) (i32.const 0))
    (call $put (call $poll_oneoff (i32.const 0) (i32.const 64) (i32.const 1) (i32.const 128)))
    (call $put (call $clock_res_get (i32.const 0) (i32.const 200)))
    (call $put (call $path_open (i32.const 3) (i32.const 0) (i32.const 300) (i32.const 1)
      (i32.const 0) (i64.const 0) (i64.const 0) (i32.const 0) (i32.const 400)))
    (call $put (call $fd_renumber (i32.const 1) (i32.const 2)))
    (call $put (call $proc_raise (i32.const 9)))))

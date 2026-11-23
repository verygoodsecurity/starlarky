;; Copies stdin to stdout, 4 KiB at a time, then returns from _start (exit 0).
(module
  (import "wasi_snapshot_preview1" "fd_read" (func $fd_read (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  ;; 0: read iovec, 16: nread, 32: write iovec, 48: nwritten, 1024..5120: buffer
  (func (export "_start")
    (local $n i32)
    (loop $again
      (i32.store (i32.const 0) (i32.const 1024))
      (i32.store (i32.const 4) (i32.const 4096))
      (if (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 16))
        (then (unreachable)))
      (local.set $n (i32.load (i32.const 16)))
      (if (i32.eqz (local.get $n)) (then (return)))
      (i32.store (i32.const 32) (i32.const 1024))
      (i32.store (i32.const 36) (local.get $n))
      (if (call $fd_write (i32.const 1) (i32.const 32) (i32.const 1) (i32.const 48))
        (then (unreachable)))
      (br $again))))

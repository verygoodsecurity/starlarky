;; Nests N calls, N being the little-endian u32 on stdin, then exits with 0. Too deep a nesting
;; traps.
(module
  (import "wasi_snapshot_preview1" "fd_read" (func $fd_read (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  (func $nest (param $n i32)
    (if (local.get $n)
      (then (call $nest (i32.sub (local.get $n) (i32.const 1))))))
  (func (export "_start")
    (i32.store (i32.const 0) (i32.const 64))
    (i32.store (i32.const 4) (i32.const 4))
    (drop (call $fd_read (i32.const 0) (i32.const 0) (i32.const 1) (i32.const 8)))
    (call $nest (i32.load (i32.const 64)))))

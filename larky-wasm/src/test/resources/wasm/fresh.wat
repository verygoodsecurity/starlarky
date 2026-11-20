;; Increments a global and a memory cell, then writes '0' + global + old cell value. A fresh
;; instance writes "1"; an instance reused from an earlier run would write "3" or more.
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  (global $runs (mut i32) (i32.const 0))
  (func (export "_start")
    (global.set $runs (i32.add (global.get $runs) (i32.const 1)))
    (i32.store8 (i32.const 100)
      (i32.add (i32.const 48) (i32.add (global.get $runs) (i32.load8_u (i32.const 101)))))
    (i32.store8 (i32.const 101) (i32.add (i32.load8_u (i32.const 101)) (i32.const 1)))
    (i32.store (i32.const 0) (i32.const 100))
    (i32.store (i32.const 4) (i32.const 1))
    (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 16)))))

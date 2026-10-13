;; Writes 4 KiB blocks of 'x' to stdout forever.
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  (func (export "_start")
    (local $i i32)
    (loop $fill
      (i32.store8 (i32.add (i32.const 4096) (local.get $i)) (i32.const 120))
      (local.set $i (i32.add (local.get $i) (i32.const 1)))
      (br_if $fill (i32.lt_u (local.get $i) (i32.const 4096))))
    (i32.store (i32.const 0) (i32.const 4096))
    (i32.store (i32.const 4) (i32.const 4096))
    (loop $forever
      (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 16)))
      (br $forever))))

;; Writes "boom" to stderr, then proc_exit(3).
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory (export "memory") 1)
  (data (i32.const 100) "boom")
  (func (export "_start")
    (i32.store (i32.const 0) (i32.const 100))
    (i32.store (i32.const 4) (i32.const 4))
    (drop (call $fd_write (i32.const 2) (i32.const 0) (i32.const 1) (i32.const 16)))
    (call $proc_exit (i32.const 3))))

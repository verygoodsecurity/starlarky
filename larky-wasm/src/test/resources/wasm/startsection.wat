;; A WASI command with a start section, which runs during instantiation: rejected.
(module
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory (export "memory") 1)
  (func $init)
  (start $init)
  (func (export "_start")))

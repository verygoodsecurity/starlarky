;; Fills all 64 MiB of its memory with random_get, forever; only a deadline or an interrupt stops it.
(module
  (import "wasi_snapshot_preview1" "random_get" (func $random_get (param i32 i32) (result i32)))
  (memory (export "memory") 1024)
  (func (export "_start")
    (loop $forever
      (drop (call $random_get (i32.const 0) (i32.const 67108864)))
      (br $forever))))

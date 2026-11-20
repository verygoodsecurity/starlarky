;; Imports random_get with the wrong signature: rejected.
(module
  (import "wasi_snapshot_preview1" "random_get" (func $random_get (param i32) (result i32)))
  (memory (export "memory") 1)
  (func (export "_start") (drop (call $random_get (i32.const 0)))))

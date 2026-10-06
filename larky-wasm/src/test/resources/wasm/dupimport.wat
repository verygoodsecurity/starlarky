;; Imports fd_write twice: first with the wrong signature, then with the right one. Invalid: every
;; import of a WASI function must have that function's signature.
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $bad (param i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_write" (func $good (param i32 i32 i32 i32) (result i32)))
  (memory (export "memory") 1)
  (func (export "_start")))

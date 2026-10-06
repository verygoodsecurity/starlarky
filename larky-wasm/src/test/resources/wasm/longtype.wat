;; A valid command whose only import's type index is 0, in one byte; the conformance tests
;; re-encode that index in longer forms.
(module
  (type $write (func (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (type $write)))
  (memory (export "memory") 1)
  (func (export "_start")))

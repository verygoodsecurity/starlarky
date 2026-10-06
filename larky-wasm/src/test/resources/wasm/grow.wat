;; Grows memory one page at a time until memory.grow returns -1, then writes the page count
;; reached to stdout in decimal and exits with it: proc_exit(pages).
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory (export "memory") 1)
  ;; Writes $v in decimal to stdout, using 2000..2048 as the digit buffer.
  (func $write_u64 (param $v i64)
    (local $p i32)
    (local.set $p (i32.const 2048))
    (loop $digit
      (local.set $p (i32.sub (local.get $p) (i32.const 1)))
      (i32.store8 (local.get $p)
        (i32.add (i32.const 48) (i32.wrap_i64 (i64.rem_u (local.get $v) (i64.const 10)))))
      (local.set $v (i64.div_u (local.get $v) (i64.const 10)))
      (br_if $digit (i64.ne (local.get $v) (i64.const 0))))
    (i32.store (i32.const 0) (local.get $p))
    (i32.store (i32.const 4) (i32.sub (i32.const 2048) (local.get $p)))
    (drop (call $fd_write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 16))))
  (func (export "_start")
    (block $done
      (loop $more
        (br_if $done (i32.eq (memory.grow (i32.const 1)) (i32.const -1)))
        (br $more)))
    (call $write_u64 (i64.extend_i32_u (memory.size)))
    (call $proc_exit (memory.size))))

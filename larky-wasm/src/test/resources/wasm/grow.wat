;; Grows memory as far as memory.grow allows, then writes the page count reached to stdout in
;; decimal and exits with it: proc_exit(pages). It tries the largest step first and halves the step
;; each time memory.grow returns -1, so it reaches the cap in about 17 calls. Growing one page at a
;; time made 1024 calls, each copying the memory on GraalWasm: 16-34s, past the isolate's 30s cap.
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
    (local $step i32)
    (local.set $step (i32.const 65536))
    (block $done
      (loop $more
        (br_if $done (i32.eqz (local.get $step)))
        (if (i32.eq (memory.grow (local.get $step)) (i32.const -1))
          (then (local.set $step (i32.shr_u (local.get $step) (i32.const 1)))))
        (br $more)))
    (call $write_u64 (i64.extend_i32_u (memory.size)))
    (call $proc_exit (memory.size))))

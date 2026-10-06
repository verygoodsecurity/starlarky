;; Traps immediately.
(module
  (memory (export "memory") 1)
  (func (export "_start")
    unreachable))

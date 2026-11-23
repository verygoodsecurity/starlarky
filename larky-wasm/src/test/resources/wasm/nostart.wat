;; Exports memory but no _start: compile must fail with INVALID_MODULE.
(module
  (memory (export "memory") 1)
  (func (export "main")))

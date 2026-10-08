;; Exports _start but no memory: compile must fail with INVALID_MODULE.
(module
  (memory 1)
  (func (export "_start")))

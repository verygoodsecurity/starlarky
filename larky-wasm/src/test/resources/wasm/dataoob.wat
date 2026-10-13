;; A data segment that ends past the module's one page of memory: instantiating it traps.
(module
  (memory (export "memory") 1)
  (data (i32.const 65530) "0123456789")
  (func (export "_start")))

;; Declares 2048 pages (128 MiB) of initial memory and does nothing else.
(module
  (memory (export "memory") 2048)
  (func (export "_start")))

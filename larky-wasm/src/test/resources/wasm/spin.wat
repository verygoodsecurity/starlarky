;; Loops forever without calling the host; only a deadline or an interrupt stops it.
(module
  (memory (export "memory") 1)
  (func (export "_start")
    (loop $forever
      (br $forever))))

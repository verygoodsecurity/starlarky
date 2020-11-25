package com.verygood.security.larky.py;

public interface PyProtocols {
  /**
      * Python interface compatibility
      * Section 3.3.1 - Basic customization
      */

  LarkyObject __new__(LarkyObject cls);

     // public void __init__();

     void __del__();

     LarkyObject __repr__();
     LarkyObject __str__();

     LarkyObject __bytes__();
     LarkyObject __format__(LarkyObject format);

     LarkyObject __lt__(LarkyObject other);
     LarkyObject __le__(LarkyObject other);
     LarkyObject __eq__(LarkyObject other);
     LarkyObject __ne__(LarkyObject other);
     LarkyObject __gt__(LarkyObject other);
     LarkyObject __ge__(LarkyObject other);

     LarkyObject __hash__();
     boolean isHashable();
     LarkyObject __bool__();

     /**
      * Section 3.3.2 - Emulating container types
      */

     // These four methods are the internal implementations of
     // attribute manipulation. They return null/false in case of
     // failure; that failure is then interpreted by the public
     // interface method.
     LarkyObject __getattr_null(java.lang.String name);
     LarkyObject __getattribute_null(java.lang.String name);
     boolean __setattr_null(java.lang.String name, LarkyObject value);
     boolean __delattr_null(java.lang.String name);

     // These four methods implement the internal interface to
     // attribute manipulation. This means they accept raw Java strings
     // as attribute names, and they raise exceptions on failure.
     // These are the methods that are invoked by VOC-generated code.
     LarkyObject __getattr__(java.lang.String name);
     LarkyObject __getattribute__(java.lang.String name);
     void __setattr__(java.lang.String name, LarkyObject value);
     void __delattr__(java.lang.String name);

     // Lastly, these methods are the public inteface to attribute
     // manipulation. This means they take Python objects as attributes,
     // and raise exceptions on failure.
     LarkyObject __getattr__(LarkyObject name);
     LarkyObject __getattribute__(LarkyObject name);
     void __setattr__(LarkyObject name, LarkyObject value);
     void __delattr__(LarkyObject name);

     // These are the prototypes for the descriptor protocol.
     LarkyObject __get__(LarkyObject instance, LarkyObject klass);
     void __set__(LarkyObject instance, LarkyObject value);
     void __delete__(LarkyObject instance);

     // Attribute name retrieval.
     LarkyObject __dir__();

     /**
      * Section 3.3.4 - Customizing instance and subclass checks
      */
     // public LarkyObject __instancecheck__();
     // public LarkyObject __subclasscheck__();

     /**
      * Section 3.3.5 - Emulating callable objects
      */
     // public LarkyObject __call__();

     /**
      * Section 3.3.6 - Emulating container types
      */

     LarkyObject __len__();
     LarkyObject __getitem__(LarkyObject item);
     void __setitem__(LarkyObject item, LarkyObject value);
     void __delitem__(LarkyObject item);
     LarkyObject __iter__();
     LarkyObject __reversed__();
     LarkyObject __contains__(LarkyObject item);

     LarkyObject __next__();

     /**
      * Section 3.3.7 - Emulating numeric types
      */

     LarkyObject __add__(LarkyObject other);
     LarkyObject __sub__(LarkyObject other);
     LarkyObject __mul__(LarkyObject other);
     LarkyObject __truediv__(LarkyObject other);
     LarkyObject __floordiv__(LarkyObject other);
     LarkyObject __mod__(LarkyObject other);
     LarkyObject __divmod__(LarkyObject other);
     LarkyObject __pow__(LarkyObject other, LarkyObject modulus);
     LarkyObject __lshift__(LarkyObject other);
     LarkyObject __rshift__(LarkyObject other);
     LarkyObject __and__(LarkyObject other);
     LarkyObject __xor__(LarkyObject other);
     LarkyObject __or__(LarkyObject other);

     LarkyObject __iadd__(LarkyObject other);
     LarkyObject __isub__(LarkyObject other);
     LarkyObject __imul__(LarkyObject other);
     LarkyObject __itruediv__(LarkyObject other);
     LarkyObject __ifloordiv__(LarkyObject other);
     LarkyObject __imod__(LarkyObject other);
     LarkyObject __idivmod__(LarkyObject other);
     LarkyObject __ipow__(LarkyObject other);
     LarkyObject __ilshift__(LarkyObject other);
     LarkyObject __irshift__(LarkyObject other);
     LarkyObject __iand__(LarkyObject other);
     LarkyObject __ixor__(LarkyObject other);
     LarkyObject __ior__(LarkyObject other);

     LarkyObject __neg__();
     LarkyObject __pos__();
     LarkyObject __abs__();
     LarkyObject __invert__();

     LarkyObject __not__();

     LarkyObject __complex__(LarkyObject real, LarkyObject imag);
     LarkyObject __int__();
     LarkyObject __float__();
     LarkyObject __round__(LarkyObject ndigits);

     LarkyObject __index__();

     // /**
     //  * Section 3.3.8 - With statement context
     //  */
     // public org.python.Object __enter__();
     // public org.python.Object __exit__(org.python.Object exc_type, org.python.Object exc_value, org.python.Object traceback);
}

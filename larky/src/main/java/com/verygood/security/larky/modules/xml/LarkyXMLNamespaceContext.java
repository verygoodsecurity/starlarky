package com.verygood.security.larky.modules.xml;

import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.verygood.security.larky.modules.types.LarkyMapping;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.NamespaceContext;



/**
 * <p>
 *   TODO: This should probably be built-in to Java already, I am assuming?
 * </p>
 *
 * see: {@link org.xml.sax.helpers.NamespaceSupport}
 */
public class LarkyXMLNamespaceContext implements LarkyMapping<String, String>, NamespaceContext {
  /**
   * see <a href="https://github.com/python/cpython/blob/3.10/Lib/xml/etree/ElementTree.py#L1013-L1026">
   *   python/cpython#ElementTree.py#L1013-L1026
   *   </a>
   *
   * see {@link javax.xml.XMLConstants}
   * see {@link com.sun.org.apache.xerces.internal.impl.Constants}
   * see {@link com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols}
   */
  private static ConcurrentNavigableMap<String, String> wellknownPrefixes() {
    ConcurrentNavigableMap<String, String> bakedInMappings = new ConcurrentSkipListMap<>();
    bakedInMappings.put(XML_NS_URI, XML_NS_PREFIX);
    bakedInMappings.put(XMLNS_ATTRIBUTE_NS_URI, XMLNS_ATTRIBUTE);
    bakedInMappings.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
    bakedInMappings.put("http://schemas.xmlsoap.org/wsdl/", "wsdl");
    // xml schema
    bakedInMappings.put("http://www.w3.org/2001/XMLSchema", "xs");
    bakedInMappings.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
    // dublin core
    bakedInMappings.put("http://purl.org/dc/elements/1.1/", "dc");
    return bakedInMappings;
  }

  private static final ConcurrentNavigableMap<String, String> WELL_KNOWN_NAMESPACE_PREFIXES = wellknownPrefixes();
  public static final LarkyXMLNamespaceContext INSTANCE = new LarkyXMLNamespaceContext();

  private final ConcurrentNavigableMap<String, String> namespaceMap;
  private final Pattern internalPatternPrefix = Pattern.compile("ns\\d+$");
  private Mutability mutability = Mutability.create(INSTANCE);
  private StarlarkThread currentThread = null;
  private int iteratorCount; // number of active iterators (unused once frozen)

  public LarkyXMLNamespaceContext() {
    this(WELL_KNOWN_NAMESPACE_PREFIXES);
  }

  public LarkyXMLNamespaceContext(ConcurrentNavigableMap<String, String> map) {
    this.namespaceMap = map;
  }

  public static LarkyXMLNamespaceContext withThread(StarlarkThread thread) {
    LarkyXMLNamespaceContext inst = INSTANCE;
    inst.setCurrentThread(thread);
    return inst;
  }

  public String getNamespaceURI(String prefix) {
    if (prefix == null) {
      throw new IllegalArgumentException("Null prefix");
    }
    if (!namespaceMap.containsKey(prefix)) {
      return NULL_NS_URI;
    }
    return namespaceMap.get(prefix);
  }

  public String getPrefix(String namespaceURI) {
    if (namespaceURI == null) {
      throw new IllegalArgumentException("Null NS URI");
    }
    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
      if (namespaceURI.equals(entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  public Iterator<String> getPrefixes(String namespaceURI) {
    if (namespaceURI == null) {
      throw new IllegalArgumentException("null namespaceURI");
    }
    List<String> results = new ArrayList<>(3);
    for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
      if (namespaceURI.equals(entry.getValue())) {
        results.add(entry.getKey());
      }
    }
    return results.iterator();
  }

  /**
   * Register a namespace prefix.
   *
   * The registry is global, and any existing mapping for either the
   * given prefix or the namespace URI will be removed.
   *
   * *prefix* is the namespace prefix, *uri* is a namespace uri. Tags and
   * attributes in this namespace will be serialized with prefix if possible.
   *
   * ValueError is raised if prefix is reserved or is invalid.
   */
  @StarlarkMethod(
    name="register_namespace",
    doc = "Register a namespace prefix.\n" +
            "\n" +
          "The registry is global, and any existing mapping for either the\n" +
          "given prefix or the namespace URI will be removed.\n" +
          "\n" +
          "*prefix* is the namespace prefix, *uri* is a namespace uri. Tags and\n" +
          "attributes in this namespace will be serialized with prefix if possible.\n" +
          "\n" +
          "ValueError is raised if prefix is reserved or is invalid.\n",
    parameters = {
      @Param(name="prefix"),
      @Param(name="uri")
  }, useStarlarkThread = true)
  public void registerNamespace(String prefix, String uri, StarlarkThread thread) throws EvalException {
    setCurrentThread(thread);
    if(getCurrentThread() == null || getCurrentThread().mutability().isFrozen()) {
      throw Starlark.errorf("Namespace map is frozen. Unable to mutate.");
    }
    if(this.internalPatternPrefix.matches(prefix)) {
      throw Starlark.errorf("ValueError: Prefix format %s reserved for internal use", prefix);
    }
    // atomic replacement
    // If there is not already a value at K, it'll just store V
    // Otherwise, it'll pass the new V and the old V to your function
    /* equivalent to the below:
      for k, v in list(_namespace_map.items()):
          if k == uri or v == prefix:
              operator.delitem(_namespace_map, k)
      _namespace_map[uri] = prefix
     */
    this.namespaceMap.merge(uri, prefix, (oldValue, newValue) -> newValue);
  }

  /**
   * We will set the current thread so that we can ensure mutability
   * across the execution StarlarkThread.
   *
   * @param thread - the starlark thread in the current execution context
   */
  public void setCurrentThread(@NotNull StarlarkThread thread) {
    if(getCurrentThread() == null || thread != getCurrentThread()) {
      this.currentThread = thread;
    }
  }

  @Override
  public StarlarkThread getCurrentThread() {
    return this.currentThread;
  }

  @Nullable
  @Override
  public Object getValue(String name) throws EvalException {
    return this.namespaceMap.get(name);
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    return ImmutableSet.copyOf(this.namespaceMap.keySet());
  }

  @Override
  public Mutability mutability() {
    return mutability;
  }

  @Override
  public void freeze() {
     mutability = Mutability.IMMUTABLE;
  }

  @Override
  public boolean updateIteratorCount(int delta) {
    if (isImmutable()) {
      return false;
    }
    if (delta > 0) {
      iteratorCount++;
    } else if (delta < 0) {
      iteratorCount--;
    }
    return iteratorCount > 0;
  }

  @Override
  public NavigableMap<String, String> contents() {
    return namespaceMap;
  }
}
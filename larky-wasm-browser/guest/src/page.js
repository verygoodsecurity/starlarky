/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Runs once in each document's Boa realm. Node identity and mutations live in DomHandle.
(() => {
  "use strict";

  const native = globalThis.__browserNative;
  const page = JSON.parse(globalThis.__browserPageInfo);
  const call = (op, ...args) => JSON.parse(native(op, JSON.stringify(args)));
  const root = call("root");
  const create = (kind, text) => wrap(call("create", kind, String(text)));
  const internal = Symbol();
  const handles = new WeakMap();
  const wrappers = new Map();
  const listeners = new WeakMap();
  const eventState = new WeakMap();
  const iframeWindows = new Map();
  const windowSources = new WeakMap();
  let activeElement = null;
  let currentScript = null;
  let readyState = page.readyState || "loading";

  function handle(node) {
    if (!handles.has(node)) throw new TypeError("Expected a DOM node");
    return handles.get(node);
  }

  function info(node) {
    return call("info", handle(node));
  }

  function wrap(id) {
    if (id == null) return null;
    if (wrappers.has(id)) return wrappers.get(id);
    const meta = call("info", id);
    if (meta === null) return null;
    const types = {
      button: HTMLButtonElement,
      form: HTMLFormElement,
      iframe: HTMLIFrameElement,
      input: HTMLInputElement,
      textarea: HTMLTextAreaElement,
    };
    const tag = meta.type === 1 ? meta.name.toLowerCase() : null;
    const Class = meta.type === 9 ? Document
      : meta.type === 3 ? Text
      : meta.type === 8 ? Comment
      : meta.type === 11 ? DocumentFragment
      : meta.type === 1 ? (Object.hasOwn(types, tag) ? types[tag] : HTMLElement)
      : Node;
    const node = new Class(internal, id);
    wrappers.set(id, node);
    return node;
  }

  class Event {
    constructor(type, options = {}) {
      this.type = String(type);
      this.bubbles = Boolean(options.bubbles);
      this.cancelable = Boolean(options.cancelable);
      this.composed = Boolean(options.composed);
      this.timeStamp = Date.now();
      eventState.set(this, {
        target: null, currentTarget: null, phase: 0, stopped: false,
        immediate: false, canceled: false, dispatching: false, trusted: false,
      });
    }
    get target() { return eventState.get(this).target; }
    get currentTarget() { return eventState.get(this).currentTarget; }
    get eventPhase() { return eventState.get(this).phase; }
    get defaultPrevented() { return eventState.get(this).canceled; }
    get isTrusted() { return eventState.get(this).trusted; }
    preventDefault() {
      if (this.cancelable) eventState.get(this).canceled = true;
    }
    stopPropagation() { eventState.get(this).stopped = true; }
    stopImmediatePropagation() {
      this.stopPropagation();
      eventState.get(this).immediate = true;
    }
    get cancelBubble() { return eventState.get(this).stopped; }
    set cancelBubble(value) { if (value) this.stopPropagation(); }
    composedPath() {
      const path = [];
      for (let node = this.target; node; node = eventParent(node)) path.push(node);
      return path;
    }
  }

  class CustomEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.detail = options.detail ?? null;
    }
  }

  class MessageEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.data = options.data ?? null;
      this.origin = options.origin ?? "";
      this.source = options.source ?? null;
      this.ports = options.ports ?? [];
    }
  }

  class InputEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.data = options.data ?? null;
      this.inputType = options.inputType ?? "";
      this.isComposing = Boolean(options.isComposing);
    }
  }

  class KeyboardEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.key = options.key ?? "";
      this.code = options.code ?? "";
      this.repeat = Boolean(options.repeat);
      this.keyCode = Number(options.keyCode ?? 0);
      this.which = this.keyCode;
      this.ctrlKey = Boolean(options.ctrlKey);
      this.metaKey = Boolean(options.metaKey);
      this.altKey = Boolean(options.altKey);
      this.shiftKey = Boolean(options.shiftKey);
    }
  }

  class DOMException extends Error {
    constructor(message = "", name = "Error") {
      super(String(message));
      this.name = String(name);
    }
  }

  class FocusEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.relatedTarget = options.relatedTarget ?? null;
    }
  }

  class MouseEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      for (const name of ["button", "buttons", "clientX", "clientY", "screenX", "screenY"]) {
        this[name] = Number(options[name] ?? 0);
      }
    }
  }

  class SubmitEvent extends Event {
    constructor(type, options = {}) {
      super(type, options);
      this.submitter = options.submitter ?? null;
    }
  }

  function eventParent(node) {
    if (node === globalThis) return null;
    if (node instanceof Document) return globalThis;
    return node instanceof Node ? node.parentNode : null;
  }

  function listenerOptions(options) {
    return options && typeof options === "object"
      ? [Boolean(options.capture), Boolean(options.once)]
      : [Boolean(options), false];
  }

  function invokeListeners(target, event, capture, phase) {
    const state = eventState.get(event);
    state.phase = phase;
    state.currentTarget = target;
    const group = listeners.get(target)?.get(event.type);

    for (const entry of group ? group.slice() : []) {
      if (state.immediate) break;
      if (entry.capture !== capture || !group.includes(entry)) continue;
      if (entry.once) group.splice(group.indexOf(entry), 1);
      if (typeof entry.callback === "function") entry.callback.call(target, event);
      else entry.callback.handleEvent?.(event);
    }

    const handler = target["on" + event.type];
    if (!capture && !state.immediate && typeof handler === "function") {
      if (handler.call(target, event) === false) event.preventDefault();
    }
  }

  class EventTarget {
    addEventListener(type, callback, options) {
      if (!callback) return;
      const eventType = String(type);
      const [capture, once] = listenerOptions(options);
      const events = listeners.get(this) || new Map();
      const group = events.get(eventType) || [];
      if (!group.some(entry => entry.callback === callback && entry.capture === capture)) {
        group.push({ callback, capture, once });
        events.set(eventType, group);
        listeners.set(this, events);
      }
    }

    removeEventListener(type, callback, options) {
      const group = listeners.get(this)?.get(String(type));
      if (!group) return;
      const [capture] = listenerOptions(options);
      const index = group.findIndex(entry => entry.callback === callback && entry.capture === capture);
      if (index !== -1) group.splice(index, 1);
    }

    dispatchEvent(event) { return dispatch(this, event, false); }
  }

  function dispatch(target, event, trusted) {
    const state = eventState.get(event);
    if (!state || state.dispatching) throw new TypeError("Invalid event");
    state.target = target;
    state.trusted = trusted;
    state.dispatching = true;
    state.stopped = false;
    state.immediate = false;
    const path = event.composedPath();
    try {
      for (let i = path.length - 1; i > 0 && !state.stopped; i--) {
        invokeListeners(path[i], event, true, 1);
      }
      if (!state.stopped) {
        invokeListeners(target, event, true, 2);
        if (!state.immediate) invokeListeners(target, event, false, 2);
      }
      for (let i = 1; event.bubbles && i < path.length && !state.stopped; i++) {
        invokeListeners(path[i], event, false, 3);
      }
    } finally {
      state.currentTarget = null;
      state.phase = 0;
      state.dispatching = false;
    }
    return !state.canceled;
  }

  function dispatchTrusted(target, event) { return dispatch(target, event, true); }

  class Node extends EventTarget {
    constructor(token, id) {
      super();
      if (token !== internal) throw new TypeError("Illegal constructor");
      handles.set(this, id);
    }
    get nodeType() { return info(this).type; }
    get nodeName() {
      const { type, name } = info(this);
      return type === 1 && this.namespaceURI === HTML ? name.toUpperCase() : name;
    }
    get ownerDocument() { return this.nodeType === Node.DOCUMENT_NODE ? null : document; }
    get parentNode() { return wrap(info(this).parent); }
    get parentElement() {
      const parent = this.parentNode;
      return parent instanceof Element ? parent : null;
    }
    get childNodes() { return info(this).children.map(wrap); }
    get firstChild() { return this.childNodes[0] ?? null; }
    get lastChild() { const children = this.childNodes; return children[children.length - 1] ?? null; }
    get previousSibling() {
      const siblings = this.parentNode?.childNodes || [];
      return siblings[siblings.indexOf(this) - 1] ?? null;
    }
    get nextSibling() {
      const siblings = this.parentNode?.childNodes || [];
      return siblings[siblings.indexOf(this) + 1] ?? null;
    }
    get isConnected() { return document.contains(this); }
    get textContent() { return call("getText", handle(this)); }
    set textContent(value) { call("setText", handle(this), value == null ? "" : String(value)); }
    get nodeValue() { return this.nodeType === 3 || this.nodeType === 8 ? this.textContent : null; }
    set nodeValue(value) {
      if (this.nodeType === 3 || this.nodeType === 8) this.textContent = value;
    }
    contains(node) {
      for (let candidate = node; candidate instanceof Node; candidate = candidate.parentNode) {
        if (candidate === this) return true;
      }
      return false;
    }
    hasChildNodes() { return this.firstChild !== null; }
    appendChild(child) { return this.insertBefore(child, null); }
    insertBefore(child, before) {
      if (child instanceof DocumentFragment) {
        for (const node of child.childNodes) this.insertBefore(node, before);
        return child;
      }
      if (before == null) call("append", handle(this), handle(child));
      else call("insert", handle(this), handle(child), handle(before));
      return child;
    }
    removeChild(child) {
      call("remove", handle(this), handle(child));
      return child;
    }
    replaceChild(child, before) {
      this.insertBefore(child, before);
      return this.removeChild(before);
    }
    getRootNode() {
      let root = this;
      while (root.parentNode) root = root.parentNode;
      return root;
    }
    cloneNode(deep = false) {
      return wrap(call("clone", handle(this), Boolean(deep)));
    }
  }
  Object.assign(Node, {
    ELEMENT_NODE: 1, TEXT_NODE: 3, COMMENT_NODE: 8, DOCUMENT_NODE: 9,
    DOCUMENT_TYPE_NODE: 10, DOCUMENT_FRAGMENT_NODE: 11,
  });
  for (const key of Object.keys(Node)) Node.prototype[key] = Node[key];

  class CharacterData extends Node {
    get data() { return this.textContent; }
    set data(value) { this.textContent = value; }
    get length() { return this.data.length; }
    remove() { this.parentNode?.removeChild(this); }
  }
  class Text extends CharacterData {}
  class Comment extends CharacterData {}

  const HTML = "http://www.w3.org/1999/xhtml";
  const propertyName = name => String(name).replace(/[A-Z]/g, letter => "-" + letter.toLowerCase());
  const words = value => String(value).match(/[^\t\n\f\r ]+/g) || [];

  function tokens(element) {
    const read = () => [...new Set(words(element.className))];
    const check = token => {
      const value = String(token);
      if (!value || words(value).join() !== value) throw new TypeError("Invalid class token");
      return value;
    };
    const write = classes => { element.className = classes.join(" "); };
    return {
      get value() { return element.className; },
      set value(value) { element.className = value; },
      get length() { return read().length; },
      item(index) { return read()[index] ?? null; },
      contains(token) { return read().includes(check(token)); },
      add(...values) { write([...new Set([...read(), ...values.map(check)])]); },
      remove(...values) {
        const removed = values.map(check);
        write(read().filter(token => !removed.includes(token)));
      },
      toggle(token, force) {
        const value = check(token);
        const enabled = force === undefined ? !this.contains(value) : Boolean(force);
        if (enabled) this.add(value);
        else this.remove(value);
        return enabled;
      },
      replace(token, replacement) {
        if (!this.contains(token)) return false;
        write([...new Set(read().map(value => value === token ? check(replacement) : value))]);
        return true;
      },
      [Symbol.iterator]() { return read()[Symbol.iterator](); },
    };
  }

  function inlineStyle(element) {
    const declarations = () => (element.getAttribute("style") || "").split(";").reduce((values, declaration) => {
      const index = declaration.indexOf(":");
      if (index !== -1) values.set(declaration.slice(0, index).trim(), declaration.slice(index + 1).trim());
      return values;
    }, new Map());
    const save = values => {
      element.setAttribute("style", [...values].map(([name, value]) => name + ": " + value).join("; "));
    };
    const style = {
      get cssText() { return element.getAttribute("style") || ""; },
      set cssText(value) { element.setAttribute("style", value); },
      getPropertyValue(name) { return declarations().get(String(name)) || ""; },
      setProperty(name, value, priority = "") {
        if (value == null || value === "") { this.removeProperty(name); return; }
        const values = declarations();
        values.set(String(name), String(value) + (priority === "important" ? " !important" : ""));
        save(values);
      },
      removeProperty(name) {
        const values = declarations();
        const previous = values.get(String(name)) || "";
        values.delete(String(name));
        save(values);
        return previous;
      },
    };
    return new Proxy(style, {
      get(target, name, receiver) {
        if (Reflect.has(target, name)) return Reflect.get(target, name, receiver);
        return typeof name === "string" ? style.getPropertyValue(propertyName(name)) : undefined;
      },
      set(target, name, value, receiver) {
        if (Reflect.has(target, name)) return Reflect.set(target, name, value, receiver);
        style.setProperty(propertyName(name), value);
        return true;
      },
    });
  }

  function attributeSet(element, name, value) {
    if (value) element.setAttribute(name, "");
    else element.removeAttribute(name);
  }

  const elementApi = {
    querySelector(selector) { return this.querySelectorAll(selector)[0] ?? null; },
    querySelectorAll(selector) {
      const id = handle(this);
      const matches = call("query", String(selector), id).map(wrap);
      matches.item = index => matches[index] ?? null;
      return matches;
    },
    getElementsByTagName(name) { return this.querySelectorAll(String(name)); },
    getElementsByClassName(name) {
      const classes = words(name);
      return classes.length ? this.querySelectorAll("[class]").filter(element =>
        classes.every(token => element.classList.contains(token))) : [];
    },
    append(...nodes) {
      for (const node of nodes) this.appendChild(node instanceof Node ? node : document.createTextNode(node));
    },
    prepend(...nodes) {
      const first = this.firstChild;
      for (const node of nodes) this.insertBefore(node instanceof Node ? node : document.createTextNode(node), first);
    },
    replaceChildren(...nodes) {
      for (const node of this.childNodes) this.removeChild(node);
      this.append(...nodes);
    },
  };

  class Element extends Node {
    constructor(token, id) {
      super(token, id);
      this.classList = tokens(this);
      this.style = inlineStyle(this);
      this.dataset = new Proxy({}, {
        get: (_, name) => typeof name === "string"
          ? this.getAttribute("data-" + propertyName(name)) ?? undefined : undefined,
        set: (_, name, value) => { this.setAttribute("data-" + propertyName(name), value); return true; },
        deleteProperty: (_, name) => { this.removeAttribute("data-" + propertyName(name)); return true; },
      });
    }
    get localName() { const meta = info(this); return meta.localName ?? meta.name.toLowerCase(); }
    get tagName() { return this.nodeName; }
    get namespaceURI() { const meta = info(this); return meta.namespaceURI ?? meta.namespace ?? HTML; }
    get children() { return this.childNodes.filter(node => node instanceof Element); }
    get firstElementChild() { return this.children[0] ?? null; }
    get lastElementChild() { const children = this.children; return children[children.length - 1] ?? null; }
    get childElementCount() { return this.children.length; }
    get id() { return this.getAttribute("id") || ""; }
    set id(value) { this.setAttribute("id", value); }
    get className() { return this.getAttribute("class") || ""; }
    set className(value) { this.setAttribute("class", value); }
    get innerHTML() { return call("getHtml", handle(this)); }
    set innerHTML(value) { call("setHtml", handle(this), String(value)); }
    getAttribute(name) { return call("getAttr", handle(this), String(name)); }
    hasAttribute(name) { return this.getAttribute(name) !== null; }
    setAttribute(name, value) { call("setAttr", handle(this), String(name), String(value)); }
    removeAttribute(name) { call("removeAttr", handle(this), String(name)); }
    setAttributeNS(namespace, name, value) {
      call("setAttr", handle(this), String(name), String(value), namespace);
    }
    removeAttributeNS(namespace, name) {
      call("removeAttr", handle(this), String(name), namespace);
    }
    toggleAttribute(name, force) {
      const enabled = force === undefined ? !this.hasAttribute(name) : Boolean(force);
      attributeSet(this, name, enabled);
      return enabled;
    }
    matches(selector) {
      if (this.isConnected) return document.querySelectorAll(selector).includes(this);
      const root = this.getRootNode();
      if (root !== this) return root.querySelectorAll(selector).includes(this);
      return call("matches", handle(this), String(selector));
    }
    closest(selector) {
      for (let element = this; element; element = element.parentElement) {
        if (element.matches(selector)) return element;
      }
      return null;
    }
    remove() { this.parentNode?.removeChild(this); }
    before(...nodes) {
      const parent = this.parentNode;
      for (const item of nodes) parent?.insertBefore(item instanceof Node ? item : document.createTextNode(item), this);
    }
    after(...nodes) {
      const parent = this.parentNode;
      const next = this.nextSibling;
      for (const item of nodes) parent?.insertBefore(item instanceof Node ? item : document.createTextNode(item), next);
    }
    getBoundingClientRect() {
      const box = call("rect", handle(this));
      return box ? new DOMRect(box.x, box.y, box.width, box.height) : new DOMRect();
    }
    getClientRects() { return this.isConnected ? [this.getBoundingClientRect()] : []; }
  }
  Object.assign(Element.prototype, elementApi);

  class HTMLElement extends Element {
    get innerText() { return this.textContent; }
    set innerText(value) { this.textContent = value; }
    get title() { return this.getAttribute("title") || ""; }
    set title(value) { this.setAttribute("title", value); }
    get hidden() { return this.hasAttribute("hidden"); }
    set hidden(value) { attributeSet(this, "hidden", value); }
    get offsetWidth() { return Math.round(this.getBoundingClientRect().width); }
    get offsetHeight() { return Math.round(this.getBoundingClientRect().height); }
    get clientWidth() { return this.offsetWidth; }
    get clientHeight() { return this.offsetHeight; }
    focus() {
      if (activeElement === this || this.disabled) return;
      activeElement?.blur();
      call("focus", handle(this));
      activeElement = this;
      this.dispatchEvent(new FocusEvent("focus"));
      this.dispatchEvent(new FocusEvent("focusin", { bubbles: true }));
    }
    blur() {
      if (activeElement !== this) return;
      activeElement = null;
      this.dispatchEvent(new FocusEvent("blur"));
      this.dispatchEvent(new FocusEvent("focusout", { bubbles: true }));
    }
    click() { activate(this, false); }
  }
  Object.defineProperties(HTMLElement.prototype, {
    onclick: { value: null, writable: true, configurable: true },
    onsubmit: { value: null, writable: true, configurable: true },
  });

  const reflected = (prototype, attributes) => {
    for (const name of attributes) {
      Object.defineProperty(prototype, name, {
        get() { return this.getAttribute(name) || ""; },
        set(value) { this.setAttribute(name, value); },
      });
    }
  };

  class HTMLInputElement extends HTMLElement {
    get value() { return call("getValue", handle(this)) ?? ""; }
    set value(value) { call("setValue", handle(this), String(value ?? "")); }
    get type() { return (this.getAttribute("type") || "text").toLowerCase(); }
    set type(value) { this.setAttribute("type", value); }
    get disabled() { return this.hasAttribute("disabled"); }
    set disabled(value) { attributeSet(this, "disabled", value); }
    get readOnly() { return this.hasAttribute("readonly"); }
    set readOnly(value) { attributeSet(this, "readonly", value); }
    get required() { return this.hasAttribute("required"); }
    set required(value) { attributeSet(this, "required", value); }
    get maxLength() { return Number(this.getAttribute("maxlength") ?? -1); }
    set maxLength(value) { this.setAttribute("maxlength", Number(value)); }
    get selectionStart() { return this._selectionStart ?? this.value.length; }
    get selectionEnd() { return this._selectionEnd ?? this.value.length; }
    set selectionStart(value) { this.setSelectionRange(value, this.selectionEnd); }
    set selectionEnd(value) { this.setSelectionRange(this.selectionStart, value); }
    setSelectionRange(start, end) {
      this._selectionEnd = Math.max(0, Math.min(Number(end), this.value.length));
      this._selectionStart = Math.max(0, Math.min(Number(start), this._selectionEnd));
      call("setSelection", handle(this), this._selectionStart, this._selectionEnd);
    }
    select() { this.setSelectionRange(0, this.value.length); }
  }
  reflected(HTMLInputElement.prototype, ["name", "placeholder", "autocomplete", "autocapitalize", "inputmode"]);

  class HTMLTextAreaElement extends HTMLInputElement {
    get type() { return "textarea"; }
  }
  class HTMLButtonElement extends HTMLElement {
    get type() { return (this.getAttribute("type") || "submit").toLowerCase(); }
    set type(value) { this.setAttribute("type", value); }
    get disabled() { return this.hasAttribute("disabled"); }
    set disabled(value) { attributeSet(this, "disabled", value); }
  }
  reflected(HTMLButtonElement.prototype, ["name", "value"]);
  class HTMLFormElement extends HTMLElement {
    get elements() { return this.querySelectorAll("input, select, button, textarea"); }
  }
  reflected(HTMLFormElement.prototype, ["method", "action", "name"]);

  function activate(element, trusted) {
    if (element.disabled) return;
    const click = new MouseEvent("click", { bubbles: true, cancelable: true });
    const proceed = trusted ? dispatchTrusted(element, click) : element.dispatchEvent(click);
    if (proceed && element.type === "submit") {
      const form = element.closest("form");
      if (form) {
        const event = new SubmitEvent("submit", {
          bubbles: true, cancelable: true, submitter: element,
        });
        if (trusted) dispatchTrusted(form, event);
        else form.dispatchEvent(event);
      }
    }
  }

  function remoteWindow(source) {
    const proxy = Object.freeze({
      postMessage(data, targetOrigin = "/") {
        if (targetOrigin && typeof targetOrigin === "object") targetOrigin = targetOrigin.targetOrigin ?? "/";
        const origin = String(targetOrigin);
        // Native routing checks the target against the actual receiving document.
        call("send", source, data ?? null, origin === "/" ? selfOrigin : origin);
      },
    });
    windowSources.set(proxy, source);
    return proxy;
  }

  class HTMLIFrameElement extends HTMLElement {
    get contentWindow() {
      const id = handle(this);
      if (!iframeWindows.has(id)) iframeWindows.set(id, remoteWindow(id));
      return iframeWindows.get(id);
    }
    get contentDocument() { return null; }
    get src() {
      const src = this.getAttribute("src");
      return src ? new URL(src, document.baseURI).href : "";
    }
    set src(value) { this.setAttribute("src", value); }
  }
  reflected(HTMLIFrameElement.prototype, ["name", "loading", "width", "height", "referrerPolicy"]);

  class DocumentFragment extends Node {}
  Object.assign(DocumentFragment.prototype, elementApi);

  class Document extends Node {
    get documentElement() { return this.querySelector("html"); }
    get head() { return this.querySelector("head"); }
    get body() { return this.querySelector("body"); }
    get activeElement() { return activeElement || this.body; }
    get readyState() { return readyState; }
    get visibilityState() { return "visible"; }
    get hidden() { return false; }
    get currentScript() { return currentScript; }
    get URL() { return location.href; }
    get documentURI() { return location.href; }
    get baseURI() {
      const base = this.querySelector("base[href]");
      return base ? new URL(base.getAttribute("href"), location.href).href : location.href;
    }
    get defaultView() { return globalThis; }
    get title() { return this.querySelector("title")?.textContent || ""; }
    set title(value) {
      let title = this.querySelector("title");
      if (!title) {
        title = this.createElement("title");
        this.head.appendChild(title);
      }
      title.textContent = value;
    }
    hasFocus() { return activeElement !== null; }
    createElement(name) { return create("element", name); }
    createElementNS(namespace, name) { return wrap(call("create", "element", String(name), namespace)); }
    createTextNode(data) { return create("text", data); }
    createComment(data) { return create("comment", data); }
    createDocumentFragment() { return create("fragment", ""); }
    getElementById(id) {
      return this.querySelectorAll("[id]").find(element => element.id === String(id)) ?? null;
    }
  }
  Object.assign(Document.prototype, elementApi);

  class DOMRect {
    constructor(x = 0, y = 0, width = 0, height = 0) {
      this.x = Number(x);
      this.y = Number(y);
      this.width = Number(width);
      this.height = Number(height);
    }
    get top() { return Math.min(this.y, this.y + this.height); }
    get bottom() { return Math.max(this.y, this.y + this.height); }
    get left() { return Math.min(this.x, this.x + this.width); }
    get right() { return Math.max(this.x, this.x + this.width); }
  }

  const decodeQuery = value => {
    try { return decodeURIComponent(value.replace(/\+/g, " ")); }
    catch { return value; }
  };
  const encodeQuery = value => encodeURIComponent(value).replace(/%20/g, "+");
  class URLSearchParams {
    constructor(init = "") {
      this._pairs = typeof init === "string"
        ? init.replace(/^\?/, "").split("&").filter(Boolean).map(pair => {
          const index = pair.indexOf("=");
          const key = index === -1 ? pair : pair.slice(0, index);
          return [decodeQuery(key), index === -1 ? "" : decodeQuery(pair.slice(index + 1))];
        })
        : Symbol.iterator in Object(init) ? [...init].map(([name, value]) => [String(name), String(value)])
        : Object.entries(init).map(([name, value]) => [name, String(value)]);
    }
    append(name, value) { this._pairs.push([String(name), String(value)]); }
    get(name) { return this._pairs.find(([key]) => key === String(name))?.[1] ?? null; }
    getAll(name) { return this._pairs.filter(([key]) => key === String(name)).map(pair => pair[1]); }
    has(name) { return this._pairs.some(([key]) => key === String(name)); }
    delete(name) { this._pairs = this._pairs.filter(([key]) => key !== String(name)); }
    set(name, value) {
      const key = String(name);
      const previous = this._pairs.findIndex(([candidate]) => candidate === key);
      if (previous === -1) this.append(key, value);
      else {
        this._pairs = this._pairs.filter(([candidate], index) => candidate !== key || index === previous);
        this._pairs[previous][1] = String(value);
      }
    }
    entries() { return this._pairs[Symbol.iterator](); }
    [Symbol.iterator]() { return this.entries(); }
    toString() { return this._pairs.map(([key, value]) => encodeQuery(key) + "=" + encodeQuery(value)).join("&"); }
  }

  class URL {
    constructor(url, base) {
      const parsed = call("resolveUrl", String(url), base == null ? null : String(base));
      Object.assign(this, parsed);
      this.searchParams = new URLSearchParams(this.search || "");
    }
    toString() { return this.href; }
    toJSON() { return this.href; }
  }

  function utf8Bytes(codePoint) {
    if (codePoint <= 0x7f) return [codePoint];
    if (codePoint <= 0x7ff) return [0xc0 | (codePoint >> 6), 0x80 | (codePoint & 0x3f)];
    if (codePoint >= 0xd800 && codePoint <= 0xdfff) codePoint = 0xfffd;
    if (codePoint <= 0xffff) return [
      0xe0 | (codePoint >> 12), 0x80 | ((codePoint >> 6) & 0x3f), 0x80 | (codePoint & 0x3f),
    ];
    return [
      0xf0 | (codePoint >> 18), 0x80 | ((codePoint >> 12) & 0x3f),
      0x80 | ((codePoint >> 6) & 0x3f), 0x80 | (codePoint & 0x3f),
    ];
  }

  class TextEncoder {
    get encoding() { return "utf-8"; }
    encode(input = "") {
      const text = String(input);
      const result = new Uint8Array(text.length * 3);
      const { written } = this.encodeInto(text, result);
      return result.slice(0, written);
    }
    encodeInto(input, destination) {
      if (!(destination instanceof Uint8Array)) throw new TypeError("Expected a Uint8Array");
      let read = 0;
      let written = 0;
      for (const character of String(input)) {
        const bytes = utf8Bytes(character.codePointAt(0));
        if (written + bytes.length > destination.length) break;
        destination.set(bytes, written);
        read += character.length;
        written += bytes.length;
      }
      return { read, written };
    }
  }

  class TextDecoder {
    constructor(label = "utf-8", options = {}) {
      if (!["utf8", "utf-8", "unicode-1-1-utf-8"].includes(String(label).trim().toLowerCase())) {
        throw new RangeError("The embedded browser currently supports UTF-8 text decoding");
      }
      this._fatal = Boolean(options.fatal);
      this._ignoreBOM = Boolean(options.ignoreBOM);
      this._pending = [];
      this._seenBOM = false;
    }
    get encoding() { return "utf-8"; }
    get fatal() { return this._fatal; }
    get ignoreBOM() { return this._ignoreBOM; }
    decode(input = new Uint8Array(), options = {}) {
      const incoming = ArrayBuffer.isView(input)
        ? new Uint8Array(input.buffer, input.byteOffset, input.byteLength)
        : input instanceof ArrayBuffer ? new Uint8Array(input) : null;
      if (incoming === null) throw new TypeError("Expected an ArrayBuffer or view");
      const bytes = new Uint8Array(this._pending.length + incoming.length);
      bytes.set(this._pending);
      bytes.set(incoming, this._pending.length);
      this._pending = [];
      const output = [];
      const emit = codePoint => {
        if (this._seenBOM || this.ignoreBOM || codePoint !== 0xfeff) {
          output.push(String.fromCodePoint(codePoint));
        }
        this._seenBOM = true;
      };
      const malformed = () => {
        if (this.fatal) {
          this._seenBOM = false;
          throw new TypeError("Malformed UTF-8");
        }
        emit(0xfffd);
      };
      for (let index = 0; index < bytes.length;) {
        const start = index;
        const lead = bytes[index++];
        if (lead <= 0x7f) { emit(lead); continue; }
        const size = lead >= 0xc2 && lead <= 0xdf ? 2
          : lead >= 0xe0 && lead <= 0xef ? 3
          : lead >= 0xf0 && lead <= 0xf4 ? 4 : 0;
        if (!size) { malformed(); continue; }
        let codePoint = lead & (0x7f >> size);
        let valid = true;
        for (let offset = 1; offset < size; offset++) {
          if (index === bytes.length) {
            if (options.stream) this._pending = [...bytes.slice(start)];
            else malformed();
            valid = false;
            break;
          }
          const minimum = offset === 1 && lead === 0xe0 ? 0xa0
            : offset === 1 && lead === 0xf0 ? 0x90 : 0x80;
          const maximum = offset === 1 && lead === 0xed ? 0x9f
            : offset === 1 && lead === 0xf4 ? 0x8f : 0xbf;
          const next = bytes[index];
          if (next < minimum || next > maximum) {
            malformed();
            valid = false;
            break;
          }
          index++;
          codePoint = (codePoint << 6) | (next & 0x3f);
        }
        if (valid) emit(codePoint);
      }
      if (!options.stream) this._seenBOM = false;
      return output.join("");
    }
  }

  class Headers {
    constructor(init = {}) {
      this._values = new Map();
      const entries = Symbol.iterator in Object(init) ? init : Object.entries(init);
      for (const [key, value] of entries) this.append(key, value);
    }
    append(name, value) {
      const key = String(name).toLowerCase();
      this._values.set(key, this.has(key) ? this.get(key) + ", " + String(value) : String(value));
    }
    get(name) { return this._values.get(String(name).toLowerCase()) ?? null; }
    has(name) { return this._values.has(String(name).toLowerCase()); }
    set(name, value) { this._values.set(String(name).toLowerCase(), String(value)); }
    delete(name) { this._values.delete(String(name).toLowerCase()); }
    entries() { return this._values.entries(); }
    [Symbol.iterator]() { return this.entries(); }
  }

  class Response {
    constructor(body = "", init = {}) {
      this._body = String(body ?? "");
      this._used = false;
      this.status = Number(init.status ?? 200);
      this.headers = new Headers(init.headers);
      this.url = init.url || "";
      this.type = init.type || "default";
    }
    get ok() { return this.status >= 200 && this.status < 300; }
    get bodyUsed() { return this._used; }
    async text() {
      if (this._used) throw new TypeError("Response already read");
      this._used = true;
      return this._body;
    }
    async json() { return JSON.parse(await this.text()); }
    async arrayBuffer() { return new TextEncoder().encode(await this.text()).buffer; }
  }

  class AbortSignal extends EventTarget {
    get aborted() { return this._aborted ?? false; }
    get reason() { return this._reason; }
    throwIfAborted() { if (this.aborted) throw this.reason; }
  }

  class AbortController {
    constructor() { this.signal = new AbortSignal(); }
    abort(reason = new DOMException("The operation was aborted", "AbortError")) {
      if (this.signal.aborted) return;
      this.signal._aborted = true;
      this.signal._reason = reason;
      this.signal.dispatchEvent(new Event("abort"));
    }
  }

  function storage() {
    const entries = new Map();
    return {
      get length() { return entries.size; },
      key(index) { return [...entries.keys()][Number(index)] ?? null; },
      getItem(key) { return entries.get(String(key)) ?? null; },
      setItem(key, value) { entries.set(String(key), String(value)); },
      removeItem(key) { entries.delete(String(key)); },
      clear() { entries.clear(); },
    };
  }

  const timers = new Map();
  let nextTimer = 0;
  function setTimer(callback, delay, repeats, args) {
    if (typeof callback !== "function") throw new TypeError("Expected a function");
    const id = ++nextTimer;
    const wait = Math.max(repeats ? 1 : 0, Math.min(Number(delay) || 0, 2147483647));
    timers.set(id, { callback, wait, due: Date.now() + wait, repeats, args });
    return id;
  }
  function pumpTimers(now = Date.now()) {
    let ran = 0;
    for (const [id, timer] of [...timers]) {
      if (timer.due > now || !timers.has(id)) continue;
      if (timer.repeats) timer.due = now + timer.wait;
      else timers.delete(id);
      timer.callback(...timer.args);
      ran++;
    }
    return ran;
  }

  const document = wrap(root);
  const location = Object.freeze(new URL(page.url));
  const parent = page.hasParent ? remoteWindow("parent") : globalThis;
  const selfOrigin = Object.hasOwn(page, "origin") ? page.origin ?? "null" : location.origin;
  const timeOrigin = Date.now();
  const width = page.viewport?.width ?? 800;
  const height = page.viewport?.height ?? 600;
  const secure = page.secureContext ?? (selfOrigin !== "null" && (location.protocol === "https:"
    || ["localhost", "127.0.0.1", "[::1]"].includes(location.hostname)));

  class Window extends EventTarget {
    static [Symbol.hasInstance](value) { return value === globalThis || windowSources.has(value); }
  }

  async function fetch(resource, options = {}) {
    const method = String(options.method ?? "GET").toUpperCase();
    if (method !== "GET" && method !== "POST") throw new TypeError("Unsupported HTTP method");
    if (method === "GET" && options.body != null) throw new TypeError("GET cannot have a body");
    options.signal?.throwIfAborted();
    const url = new URL(resource, document.baseURI);
    const sameOrigin = url.origin === selfOrigin;
    if (!sameOrigin && options.mode === "same-origin") throw new TypeError("Cross-origin fetch denied");

    const payload = method === "POST" ? String(options.body ?? "") : null;
    const response = call("fetch", url.href, payload);
    if (!sameOrigin && options.mode === "no-cors") {
      return new Response(null, { status: 0, type: "opaque" });
    }
    if (!sameOrigin) {
      const headers = new Headers(response?.headers);
      const allowed = headers.get("access-control-allow-origin");
      const includeCredentials = options.credentials === "include";
      if ((allowed !== selfOrigin && (allowed !== "*" || includeCredentials))
        || (includeCredentials && headers.get("access-control-allow-credentials") !== "true")) {
        throw new TypeError("Cross-origin response unavailable");
      }
    }
    return typeof response === "string"
      ? new Response(response, { url: url.href })
      : new Response(response.body, { status: response.status, headers: response.headers, url: response.url || url.href });
  }

  Object.assign(globalThis, {
    window: globalThis, self: globalThis, top: parent, parent, document, location, origin: selfOrigin,
    Window, EventTarget, Event, CustomEvent, MessageEvent, InputEvent, KeyboardEvent,
    MouseEvent, SubmitEvent, FocusEvent, DOMRect, DOMException,
    TextEncoder, TextDecoder, Headers, Response, AbortSignal, AbortController, fetch,
    isSecureContext: secure, performance: { timeOrigin, now: () => Date.now() - timeOrigin },
    innerWidth: width, outerWidth: width, innerHeight: height, outerHeight: height, devicePixelRatio: 1,
    screen: {
      width, height, availWidth: width, availHeight: height, colorDepth: 24, pixelDepth: 24,
    },
    Node, Element, Document, HTMLDocument: Document, HTMLElement, Text, Comment, DocumentFragment,
    HTMLInputElement, HTMLTextAreaElement, HTMLIFrameElement, HTMLButtonElement, HTMLFormElement,
    URL, URLSearchParams,
    navigator: Object.freeze({
      userAgent: page.userAgent || "Kitesurf/WASI", language: "en-US", languages: ["en-US"],
      platform: "", cookieEnabled: false,
    }),
    addEventListener: EventTarget.prototype.addEventListener.bind(globalThis),
    removeEventListener: EventTarget.prototype.removeEventListener.bind(globalThis),
    dispatchEvent: EventTarget.prototype.dispatchEvent.bind(globalThis),
    postMessage(data, targetOrigin = "/") {
      if (targetOrigin && typeof targetOrigin === "object") targetOrigin = targetOrigin.targetOrigin ?? "/";
      const origin = String(targetOrigin);
      if (origin !== "*" && origin !== "/" && origin !== selfOrigin) return;
      const serialized = JSON.stringify(data);
      setTimer(() => dispatchTrusted(globalThis, new MessageEvent("message", {
        data: serialized === undefined ? null : JSON.parse(serialized),
        origin: selfOrigin,
        source: globalThis,
      })), 0, false, []);
    },
    getComputedStyle(element) { return element.style; },
    localStorage: storage(), sessionStorage: storage(),
    setTimeout(callback, delay, ...args) { return setTimer(callback, delay, false, args); },
    setInterval(callback, delay, ...args) { return setTimer(callback, delay, true, args); },
    clearTimeout(id) { timers.delete(Number(id)); },
    clearInterval(id) { timers.delete(Number(id)); },
    requestAnimationFrame(callback) { return setTimer(() => callback(Date.now()), 16, false, []); },
    cancelAnimationFrame(id) { timers.delete(Number(id)); },
    __browserReady() {
      if (readyState === "complete") return;
      readyState = "interactive";
      dispatchTrusted(document, new Event("DOMContentLoaded", { bubbles: true }));
      readyState = "complete";
      dispatchTrusted(globalThis, new Event("load"));
    },
    __browserInput(id, data = null) {
      const target = wrap(id);
      target.focus();
      if (target instanceof HTMLInputElement) {
        target.setSelectionRange(target.value.length, target.value.length);
      }
      dispatchTrusted(target, new InputEvent("input", { bubbles: true, inputType: "insertText", data }));
    },
    __browserChange(id) { dispatchTrusted(wrap(id), new Event("change", { bubbles: true })); },
    __browserKeydown(id, key) {
      const target = wrap(id);
      target.focus();
      const keyCode = /^[A-Za-z0-9]$/.test(key) ? key.toUpperCase().charCodeAt(0) : 0;
      return dispatchTrusted(target, new KeyboardEvent("keydown", {
        key, keyCode, bubbles: true, cancelable: true,
      }));
    },
    __browserClick(id) { activate(wrap(id), true); },
    __browserCurrentScript(id) { currentScript = wrap(id); },
    __browserFrameLoaded(id) {
      dispatchTrusted(wrap(id), new Event("load"));
    },
    __browserMessage(data, origin, source) {
      const sender = source === "parent" ? parent : wrap(source)?.contentWindow ?? null;
      dispatchTrusted(globalThis, new MessageEvent("message", { data, origin, source: sender }));
    },
    __browserPumpTimers: pumpTimers,
  });

  delete globalThis.__browserNative;
  delete globalThis.__browserPageInfo;
})();

(() => {
  "use strict";
  const native = globalThis.__browserNative;
  const call = (op, args) => JSON.parse(native(op, JSON.stringify(args)));
  const keyStore = new WeakMap();
  const keyConstructorToken = Symbol();
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

  class CryptoKey {
    constructor(token, algorithm, extractable, type, usages) {
      if (token !== keyConstructorToken) throw new TypeError("Illegal constructor");
      Object.defineProperties(this, {
        algorithm: { value: Object.freeze(algorithm), enumerable: true },
        extractable: { value: Boolean(extractable), enumerable: true },
        type: { value: type, enumerable: true },
        usages: { value: Object.freeze(usages), enumerable: true },
      });
      Object.freeze(this);
    }
  }

  const decode64 = (input) => {
    const encoded = String(input).replace(/[\t\n\f\r ]/g, "");
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(encoded) || encoded.length % 4 === 1 ||
        (encoded.includes("=") && encoded.length % 4 !== 0)) {
      throw new TypeError("Invalid base64");
    }
    let bits = 0, count = 0, out = "";
    for (const ch of encoded.replace(/=+$/, "")) {
      bits = (bits << 6) | alphabet.indexOf(ch);
      count += 6;
      if (count >= 8) {
        count -= 8;
        out += String.fromCharCode((bits >> count) & 255);
      }
    }
    return out;
  };

  const encode64 = (input) => {
    const value = String(input);
    let ret = "";
    for (let at = 0; at < value.length; at += 3) {
      const a = value.charCodeAt(at);
      const b = value.charCodeAt(at + 1) || 0;
      const c = value.charCodeAt(at + 2) || 0;
      if ((a | b | c) > 255) throw new TypeError("Invalid binary string");
      ret += alphabet[a >> 2] + alphabet[((a & 3) << 4) | (b >> 4)];
      ret += at + 1 < value.length ? alphabet[((b & 15) << 2) | (c >> 6)] : "=";
      ret += at + 2 < value.length ? alphabet[c & 63] : "=";
    }
    return ret;
  };

  const bytes = (input) => {
    if (input instanceof ArrayBuffer) return Array.from(new Uint8Array(input));
    if (ArrayBuffer.isView(input)) {
      return Array.from(new Uint8Array(input.buffer, input.byteOffset, input.byteLength));
    }
    throw new TypeError("Expected a BufferSource");
  };
  const algorithmName = (algorithm) => String(algorithm?.name ?? algorithm).toUpperCase();
  const jwkPart = (value) => {
    if (typeof value !== "string" || !/^[A-Za-z0-9_-]+$/.test(value)) {
      throw new TypeError("Invalid RSA public key");
    }
    return Array.from(decode64(value.replace(/-/g, "+").replace(/_/g, "/")), (char) => char.charCodeAt(0));
  };

  const subtle = Object.freeze({
    async importKey(format, data, algorithm, extractable, usages) {
      const name = algorithmName(algorithm);
      const requested = Array.from(usages || []);
      const supported = name === "RSA-OAEP" ? ["encrypt", "wrapKey"] : ["encrypt"];
      const normalizedUsages = [...new Set(requested)];
      if (!normalizedUsages.length || normalizedUsages.some(usage => !supported.includes(usage))) {
        throw new TypeError("Unsupported key usage");
      }
      let material, type, normalized;
      if (format === "raw" && name === "AES-GCM") {
        const raw = bytes(data);
        if (![16, 24, 32].includes(raw.length)) throw new TypeError("Invalid AES key length");
        material = { raw };
        type = "secret";
        normalized = { name, length: raw.length * 8 };
      } else if (format === "jwk" && name === "RSA-OAEP" &&
                 algorithmName(algorithm?.hash) === "SHA-256" && data?.kty === "RSA" &&
                 data.d === undefined && (data.use === undefined || data.use === "enc") &&
                 (data.alg === undefined || data.alg === "RSA-OAEP-256") &&
                 (data.key_ops === undefined || (Array.isArray(data.key_ops) && normalizedUsages.every(usage => data.key_ops.includes(usage)))) &&
                 (data.ext !== false || !extractable)) {
        const n = jwkPart(data.n);
        const e = jwkPart(data.e);
        if (!n[0] || !e[0]) throw new TypeError("Invalid RSA public key");
        material = { n, e };
        type = "public";
        normalized = {
          name, hash: Object.freeze({ name: "SHA-256" }),
          modulusLength: (n.length - 1) * 8 + 32 - Math.clz32(n[0]),
          publicExponent: Uint8Array.from(e),
        };
      } else {
        throw new TypeError("Unsupported key algorithm");
      }
      const key = new CryptoKey(keyConstructorToken, normalized, extractable, type, normalizedUsages);
      keyStore.set(key, material);
      return key;
    },
    async encrypt(algorithm, key, plaintext) {
      const material = keyStore.get(key);
      const name = algorithmName(algorithm);
      if (!material || key.algorithm.name !== name || !key.usages.includes("encrypt")) {
        throw new TypeError("Invalid encryption key");
      }
      const input = bytes(plaintext);
      if (name === "AES-GCM") {
        const aad = algorithm.additionalData === undefined ? [] : bytes(algorithm.additionalData);
        return Uint8Array.from(call("aesGcm", [
          material.raw, bytes(algorithm.iv), aad, algorithm.tagLength ?? 128, input
        ])).buffer;
      }
      if (algorithm?.label !== undefined && bytes(algorithm.label).length) {
        throw new TypeError("Unsupported RSA-OAEP label");
      }
      return Uint8Array.from(call("rsaOaep256", [material.n, material.e, input])).buffer;
    },
    async wrapKey(format, key, wrappingKey, algorithm) {
      const material = keyStore.get(key);
      const wrapping = keyStore.get(wrappingKey);
      if (format !== "raw" || !material?.raw || !key.extractable || !wrapping?.n ||
          wrappingKey.algorithm.name !== "RSA-OAEP" || algorithmName(algorithm) !== "RSA-OAEP" ||
          !wrappingKey.usages.includes("wrapKey") ||
          (algorithm?.label !== undefined && bytes(algorithm.label).length)) {
        throw new TypeError("Invalid wrapping key");
      }
      return Uint8Array.from(call("rsaOaep256", [wrapping.n, wrapping.e, material.raw])).buffer;
    },
  });

  if (!globalThis.atob) globalThis.atob = decode64;
  if (!globalThis.btoa) globalThis.btoa = encode64;
  globalThis.CryptoKey = CryptoKey;
  globalThis.crypto = Object.freeze({
    getRandomValues(view) {
      const tag = Object.prototype.toString.call(view);
      if (!ArrayBuffer.isView(view) || !/^\[object (?:Uint8Clamped|Uint8|Uint16|Uint32|Int8|Int16|Int32|BigInt64|BigUint64)Array\]$/.test(tag)) {
        throw new TypeError("Expected an integer typed array");
      }
      if (view.byteLength > 65536) throw new TypeError("Random request too large");
      new Uint8Array(view.buffer, view.byteOffset, view.byteLength).set(call("random", [view.byteLength]));
      return view;
    },
    subtle,
  });
})();

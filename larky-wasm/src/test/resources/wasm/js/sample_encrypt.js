// A stand-in for a payment processor's encrypt.js, used by the @vgs//wasm conformance tests.
//
// Reads {"pan": "...", "key": "..."} as JSON from stdin and writes
// {"encrypted": "...", "keyId": "..."} as JSON to stdout. The transform is deterministic and keeps
// the PAN's format: separators stay where they are, every digit stays a digit, and the last four
// digits are left alone. It is NOT encryption; it only exercises QuickJS (JSON, strings, arrays,
// typed arrays, Math) the way a real vendor script would.
//
// On invalid input it writes the reason to stderr and {"error": "..."} to stdout. Javy has no way
// to choose an exit code (an uncaught exception traps the module), so errors are reported in the
// output instead.
//
// Build: larky-wasm/tools/build-fixtures.sh (javy build, static).

function readStdin() {
  const chunks = [];
  let total = 0;
  for (;;) {
    const buffer = new Uint8Array(4096);
    const n = Javy.IO.readSync(0, buffer);
    if (n === 0) {
      break;
    }
    chunks.push(buffer.subarray(0, n));
    total += n;
  }
  const all = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    all.set(chunk, offset);
    offset += chunk.length;
  }
  return new TextDecoder().decode(all);
}

function write(fd, text) {
  Javy.IO.writeSync(fd, new TextEncoder().encode(text));
}

// FNV-1a over the key's UTF-16 code units, as an unsigned 32-bit integer.
function hashKey(key) {
  let h = 0x811c9dc5;
  for (let i = 0; i < key.length; i++) {
    h ^= key.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return h >>> 0;
}

// xorshift32 keystream seeded from the key.
function keystream(seed) {
  let state = seed || 0x9e3779b9;
  return function next() {
    state ^= state << 13;
    state ^= state >>> 17;
    state ^= state << 5;
    state >>>= 0;
    return state;
  };
}

function encrypt(pan, key) {
  const chars = pan.split("");
  const digitPositions = [];
  chars.forEach((c, i) => {
    if (c >= "0" && c <= "9") {
      digitPositions.push(i);
    }
  });
  const next = keystream(hashKey(key));
  const keep = digitPositions.slice(-4);
  let previous = 0;
  for (const position of digitPositions) {
    if (keep.indexOf(position) >= 0) {
      continue;
    }
    const d = chars[position].charCodeAt(0) - 48;
    const shift = (next() % 10 + previous) % 10;
    const e = (d + shift) % 10;
    chars[position] = String.fromCharCode(48 + e);
    previous = Math.floor((e * 7 + d) % 10);
  }
  return chars.join("");
}

function validate(request) {
  if (request === null || typeof request !== "object" || Array.isArray(request)) {
    return "input must be a JSON object";
  }
  if (typeof request.pan !== "string") {
    return "pan must be a string";
  }
  if (typeof request.key !== "string" || request.key.length === 0) {
    return "key must be a non-empty string";
  }
  if (!/^[0-9 -]+$/.test(request.pan)) {
    return "pan may contain only digits, spaces and dashes";
  }
  const digits = request.pan.replace(/[^0-9]/g, "").length;
  if (digits < 12 || digits > 19) {
    return "pan must have 12 to 19 digits; got " + digits;
  }
  return null;
}

function main() {
  let request;
  try {
    request = JSON.parse(readStdin());
  } catch (e) {
    return { error: "input is not JSON: " + e.message };
  }
  const problem = validate(request);
  if (problem !== null) {
    return { error: problem };
  }
  const keyId = ("00000000" + hashKey(request.key).toString(16)).slice(-8);
  return { encrypted: encrypt(request.pan, request.key), keyId: keyId };
}

const result = main();
if (result.error !== undefined) {
  write(2, "sample_encrypt: " + result.error + "\n");
}
write(1, JSON.stringify(result));

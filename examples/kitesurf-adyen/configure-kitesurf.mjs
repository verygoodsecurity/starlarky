import { copyFileSync, readFileSync, writeFileSync } from 'node:fs';

function replace(file, before, after) {
  const contents = readFileSync(file, 'utf8');
  if (contents.split(before).length !== 2) throw new Error(`Unexpected Kitesurf source in ${file}`);
  writeFileSync(file, contents.replace(before, () => after));
}

// Three secured inputs and an ancillary frame can be live at once.
const budget = 'packages/engine/src/eval/worker-budget.ts';
replace(budget, 'MAX_DYNAMIC_WORKERS = 4;', 'MAX_DYNAMIC_WORKERS = 8;');
replace(budget, 'MAX_CONCURRENT_OOPIF = 1;', 'MAX_CONCURRENT_OOPIF = 4;');

const globals = 'packages/page-script/src/globals.ts';
const anchor = '        {\n          const nativeGenerateKey';
const adapter = `        {
          const nativeImportKey = subtle.importKey as (...args: unknown[]) => Promise<unknown>;
          Object.defineProperty(subtle, "importKey", {
            value: function importKey(...args: unknown[]): Promise<unknown> {
              if (args[0] !== "jwk") args[1] = ks_bridge_buffer_bytes(args[1]) ?? args[1];
              if (args.length >= 5 && args[4] != null) args[4] = toKeyUsages(args[4] as KeyUsageInput);
              return nativeImportKey(...args);
            },
            writable: true,
            enumerable: true,
            configurable: true,
          });
        }

        {
          const nativeEncrypt = subtle.encrypt.bind(subtle);
          const bytes = (value: unknown) => ks_bridge_buffer_bytes(value) ?? value;
          Object.defineProperty(subtle, "__adyenDemoEncryptBytes", {
            value: async function encryptBytes(algorithm: unknown, key: CryptoKey, data: unknown) {
              if (typeof algorithm === "object" && algorithm !== null) {
                const params = { ...algorithm } as Record<string, unknown>;
                for (const name of ["iv", "additionalData", "label", "counter"]) {
                  if (params[name] !== undefined) params[name] = bytes(params[name]);
                }
                algorithm = params;
              }
              const encrypted = await nativeEncrypt(algorithm as AlgorithmIdentifier, key, bytes(data) as BufferSource);
              return Array.from(new Uint8Array(encrypted));
            },
            configurable: true,
          });
        }

`;
replace(globals, anchor, adapter + anchor);

copyFileSync(new URL('./engine.ts', import.meta.url), 'packages/engine/src/adyen-demo.ts');
replace('packages/engine/wrangler.jsonc', '"main": "src/index.ts"', '"main": "src/adyen-demo.ts"');

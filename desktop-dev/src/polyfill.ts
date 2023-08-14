import { webcrypto } from "node:crypto";
if (typeof globalThis.crypto === "undefined") {
  Object.assign(globalThis, { crypto: webcrypto });
}

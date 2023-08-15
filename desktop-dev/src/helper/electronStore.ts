import { resolveToDataRoot } from "./createResolveTo.ts";

import { decode, encode } from "cbor-x";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

export class Store<T extends { [key: string]: any }> {
  constructor(readonly name: string, options: { secret?: { key: Uint8Array; iv: Uint8Array } } = {}) {
    this._dataDir = resolveToDataRoot("store", this.name);
    console.log("存储位置：",this._dataDir)
    /// 默认情况下，密钥使用 模块名称 进行加密
    this.#secretKey =
      options.secret?.key ??
      crypto
        .createHash("sha256")
        .update(this.name + ".key.store.dweb")
        .digest();

    /// 默认情况下，初始化向量使用 当前的用户信息 进行加密
    this.#secretIv =
      options.secret?.iv ??
      crypto
        .createHash("sha256")
        .update(JSON.stringify(os.userInfo()) + ".iv.store.dweb")
        .digest()
        .subarray(16);
  }
  private readonly _dataDir: string;
  readonly #secretKey: Uint8Array;
  readonly #secretIv: Uint8Array;
  /** 数据编码与加密 */
  #encode(value: any) {
    const data = encode(value);
    const cipher = crypto.createCipheriv("aes-256-cbc", this.#secretKey, this.#secretIv);
    const sdata = Buffer.concat([cipher.update(data), cipher.final()]);
    return sdata;
  }
  /** 数据解密与解码 */
  #decode(sdata: Uint8Array) {
    const decipher = crypto.createDecipheriv("aes-256-cbc", this.#secretKey, this.#secretIv);
    const data = Buffer.concat([decipher.update(sdata), decipher.final()]);
    return decode(data);
  }
  private _resolveKey(key: string, autoCreate = true) {
    const filepath = path.resolve(this._dataDir, key + ".cbor");
    if (autoCreate && fs.existsSync(filepath) === false) {
      fs.mkdirSync(path.dirname(filepath), { recursive: true });
      fs.writeFileSync(filepath, Buffer.alloc(0));
    }
    return filepath;
  }
  get<K extends keyof T & string>(key: K, orDefault: () => T[K]): T[K];
  get<K extends keyof T & string>(key: K, orDefault?: () => T[K]): T[K] | undefined;
  get<K extends keyof T & string>(key: K, orDefault?: () => T[K]) {
    try {
      const data = fs.readFileSync(this._resolveKey(key));
      return this.#decode(data) as T[K];
    } catch {
      if (orDefault) {
        const defaultValue = orDefault();
        if (this.set(key, defaultValue)) {
          return defaultValue;
        }
        throw new Error(`fail to save store for '${this.name}'`);
      }
      return undefined;
    }
  }
  set<K extends keyof T & string>(key: K, value: T[K]) {
    try {
      const data = this.#encode(value);
      fs.writeFileSync(this._resolveKey(key), data);
      return true;
    } catch {
      return false;
    }
  }
  delete<K extends keyof T & string>(key: K) {
    try {
      fs.unlinkSync(this._resolveKey(key, false));
      return true;
    } catch {
      return false;
    }
  }
  clear() {
    fs.rmSync(this._dataDir, { recursive: true });
  }
}

export const electronConfig = new Store<ElectronConfig>("config");
declare global {
  // deno-lint-ignore no-empty-interface
  interface ElectronConfig {}
}

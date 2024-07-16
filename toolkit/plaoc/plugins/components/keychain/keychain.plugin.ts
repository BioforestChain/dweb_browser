import { bytesToBase64 } from "@dweb-browser/helper/encoding.ts";
import { fetchBaseExtends } from "@dweb-browser/helper/fetchExtends/$makeFetchBaseExtends.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";

export class KeyChainPlugin extends BasePlugin {
  constructor() {
    super("keychain.sys.dweb");
  }

  @bindThis
  async keys(): Promise<string[]> {
    return await this.fetchApi("/keys").object<string[]>();
  }
  @bindThis
  async get(key: string): Promise<Uint8Array | undefined> {
    const res = await this.fetchApi("/get", { search: { key } });
    if (res.status === 404) {
      return;
    }
    await fetchBaseExtends.ok.call(res);
    return new Uint8Array(await res.arrayBuffer());
  }
  @bindThis
  async set(key: string, data: Uint8Array | string): Promise<boolean> {
    let encoding: string | undefined;
    let value: string;
    if (typeof data == "string") {
      value = data;
    } else {
      value = bytesToBase64(data);
    }
    return await this.fetchApi("/set", { search: { key, value, encoding } }).object<boolean>();
  }
  @bindThis
  async delete(key: string): Promise<boolean> {
    return await this.fetchApi("/delete", { search: { key } }).object<boolean>();
  }
  @bindThis
  async has(key: string): Promise<boolean> {
    return await this.fetchApi("/has", { search: { key } }).object<boolean>();
  }
}

export const keychainPlugin = new KeyChainPlugin();

/**
 *  plaoc config set bundle-web-hook {http-url}
 *  plaoc config set bundle-web-hook-headers {JSON-KEY-VALUE}
 */

import { Command, EnumType } from "../deps/cliffy.ts";

export enum EHook {
  webadvServerUrl = "webadv-server-url",
  webadbServerAuth = "webadv-server-auth",
  bundleWebHook = "bundle-web-hook",
  bundleWebHookHeaders = "bundle-web-hook-headers",
}
const hookType = new EnumType(EHook);
export const doConfigCommand = new Command()
  .type("hookType", hookType)
  .arguments("<set:string> <type:hookType> <key:string>")
  .description("Set up a hook to deploy to the server.")
  .env("PLAOC_ENV_HOOK=<value:string>", "Webhook environment variables.")
  .action((options, set, hooks, key) => {
    if (set === "set") {
      //TODO
      if (hooks === EHook.bundleWebHook) {
        return bundleWebHook(key);
      }
      //TODO
      if (hooks === EHook.bundleWebHookHeaders) {
        let headersHookKey = "";
        // 如果是$开头的字段，那么就会读取相对应的环境变量
        if (key.startsWith("$")) {
          key = key.slice(1);
          const value = Deno.env.get(key);
          if (value) headersHookKey = value;
        }
        //  如果没有找到环境变量,并且没有设置PLAOC_ENV_HOOK,就使用空字符串
        if (options.plaocEnvHook) {
          headersHookKey = options.plaocEnvHook;
        }
        return bundleWebHookHeaders(headersHookKey);
      }
    }

    throw new Error(`not found command for config ${set} ${hooks} ${key}.please use --help.`);
  });

const bundleWebHook = (_data: string) => {};
const bundleWebHookHeaders = (_data: string) => {};

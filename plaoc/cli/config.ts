/**
 *  plaoc config set bundle-web-hook {http-url}
 *  plaoc config set bundle-web-hook-headers {JSON-KEY-VALUE}
 */

import { Command, EnumType } from "./deps.ts";

export enum EHook {
  bundleWebHook = "bundle-web-hook",
  bundleWebHookHeaders = "bundle-web-hook-headers"
}
const hookType = new EnumType(EHook);
export const doConfigFlags =  new Command()
.type("hookType",hookType)
.arguments("<set:string> <hooks:hookType> <key:string>")
.description("Packaged items.")
.action((_options,set,hooks,key) => {
  if (set === "set")  {
    if (hooks === EHook.bundleWebHook) {
      return bundleWebHook(key)
    }
    if (hooks === EHook.bundleWebHookHeaders) {
      return bundleWebHookHeaders(key)
    }
  }

  throw new Error(`not found command for config ${set} ${hooks} ${key}.please use --help.`) 
});

const bundleWebHook = (_data:string) => {
  
}
const bundleWebHookHeaders = (_data:string) => {
  
}
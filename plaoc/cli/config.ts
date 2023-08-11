/**
 *  plaoc config set bundle-web-hook {http-url}
 *  plaoc config set bundle-web-hook-headers {JSON-KEY-VALUE}
 */

export const doConfig = async (args = Deno.args) => {
  const [set, ...follow] = args;
  if (set === EHook.set) {
   return warpperWebHook(follow)
  }

  throw new Error("");
}

const warpperWebHook = (hooks:string[]) => {
  console.log("doConfig=>",hooks)
  const [hookHandle,...follow] = hooks;
  if (hookHandle === EHook.bundleWebHook) {
    return bundleWebHook(follow)
  }
  if (hookHandle === EHook.bundleWebHookHeaders) {
    return bundleWebHookHeaders(follow)
  }
}

const bundleWebHook = (data:string[]) => {
  
}
const bundleWebHookHeaders = (data:string[]) => {
  
}

export enum EHook {
  set = "set",
  bundleWebHook = "bundle-web-hook",
  bundleWebHookHeaders = "bundle-web-hook-headers"
}

if (import.meta.main) {
  doConfig()
}

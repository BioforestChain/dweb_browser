// @ts-check
/// <reference lib="dom"/>
const setupFetch = () => {
  const nativeFetch = fetch;
  const dwebFetch: typeof fetch = (input: URL | RequestInfo, init?: RequestInit) => {
    return nativeFetch(new DwebRequest(input, init));
  };

  const NativeRequest = Request;
  class DwebRequest extends NativeRequest {
    constructor(input: RequestInfo | URL, init?: RequestInit) {
      let inputUrl: URL | undefined;
      if (input instanceof URL) {
        inputUrl = input;
      } else if (typeof input === "string") {
        inputUrl = new URL(input);
      }
      if (inputUrl !== undefined) {
        if (inputUrl.username) {
          const dwebHeaders = new Headers(init?.headers);
          dwebHeaders.set("X-Dweb-Host", decodeURIComponent(inputUrl.username));
          init = {
            ...init,
            headers: dwebHeaders,
          };
        }
        inputUrl.username = "";
        input = inputUrl;
      }
      super(input, init);
    }
  }

  const G = typeof globalThis === "object" ? globalThis : self;

  Object.assign(G, {
    fetch: dwebFetch,
    Request: DwebRequest,
    dwebShim: { nativeFetch, NativeRequest },
  });
};
// const setupWorker = ()=>{
//     class DwebWorker extends Worker{
//         constructor(scriptURL: string | URL, options?: WorkerOptions | undefined){
//         }
//     }
//     new Worker
// }

red:{
    "/crypto.js":{
        to:"crypto.js?dweb-shim=fetch"
    }
}
/*#__PURE__*/
export const setupFetch = () => {
  const nativeFetch = fetch;
  const dwebFetch: typeof fetch = (input: URL | RequestInfo, init?: RequestInit) => {
    return nativeFetch(new DwebRequest(input, init));
  };
  const getBaseUrl = typeof document === 'object'?()=>document.baseURI:()=>location.href;

  const NativeRequest = Request;
  class DwebRequest extends NativeRequest {
    constructor(input: RequestInfo | URL, init?: RequestInit) {
      let inputUrl: URL | undefined;
      if (input instanceof URL) {
        inputUrl = input;
      } else if (typeof input === "string") {
        inputUrl = new URL(input,getBaseUrl());
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
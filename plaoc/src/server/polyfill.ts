/**
 * https://github.com/ungap/random-uuid/blob/main/index.js
 */
if (typeof crypto.randomUUID !== "function") {
  crypto.randomUUID = function randomUUID() {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (_c) => {
      const c = +_c;
      return (c ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (c / 4)))).toString(16);
    }) as never;
  };
}
if (typeof Response.json !== "function") {
  Response.json = (data: unknown, init: ResponseInit = {}) => {
    const headers = new Headers(init.headers);
    headers.set("Content-Type", "application/json");
    return new Response(JSON.stringify(data), { ...init, headers });
  };
}

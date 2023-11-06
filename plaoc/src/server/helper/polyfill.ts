if (typeof Response.json !== "function") {
  Response.json = (data: unknown, init: ResponseInit = {}) => {
    const headers = new Headers(init.headers);
    headers.set("Content-Type", "application/json");
    return new Response(JSON.stringify(data), { ...init, headers });
  };
}

(async () => {
  if (typeof URLPattern === 'undefined') {
    await import("npm:urlpattern-polyfill");
  }
})();

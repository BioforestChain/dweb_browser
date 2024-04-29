declare const URLPattern: unknown;
if (typeof URLPattern === "undefined") {
  // top await 在 chrome 89 已经得到支持
  await import("urlpattern-polyfill");
}
export {};

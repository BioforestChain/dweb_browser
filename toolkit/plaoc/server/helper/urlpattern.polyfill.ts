if (typeof URLPattern === "undefined") {
  /// 这个文件是使用 esbuild 编译到 dist 里头的，这里这样写是为了绕开 esbuild 的编译
  // const urlpattern_polyfill_spec = ;
 importScripts("urlpattern.polyfill.js");
}
export {};
declare const URLPattern: unknown;

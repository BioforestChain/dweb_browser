//!此处为js ipc特有垫片，防止有些webview版本过低，出现无法支持的函数

if (typeof crypto.randomUUID !== "function") {
  crypto.randomUUID = function randomUUID() {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (_c) => {
      const c = +_c;
      return (c ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (c / 4)))).toString(16);
    }) as never;
  };
}
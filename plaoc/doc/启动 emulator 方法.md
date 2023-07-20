## 启动 emulator 方法

### 启动 
- plaoc/src/server/http-www-server.ts
```ts
const remoteIpcResponse = await jsProcess.nativeRequest(
  `file:///usr/${root}${pathname}?mode=stream`
);

```


### 不启动
- plaoc/src/server/http-www-server.ts
```ts
const remoteIpcResponse = await jsProcess.nativeRequest(
  `file:///sys/plaoc-demo${pathname}?mode=stream`
);

```

### 自适应
- plaoc/src/server/http-www-server.ts
```ts
const remoteIpcResponse = await jsProcess.nativeRequest(
  `file:///${root === "www" ? "sys" : "usr"}/${root === "www" ? "plaoc-demo" : root}${pathname}?mode=stream`
);

```


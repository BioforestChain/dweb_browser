# 工程中运行 plugin/demo 应用流程

- `deno task assets --dev` 开启监听
  编译 dweb_browser/desktop-dev
- `deno task dnt`
  deno 代码会被转换成 nodejs 代码，并输出在 electron 目录下，完成 commonjs 的编译，之后会启动 electron。
  已经做了 source-map 的映射，可以在 deno 代码中直接打断点进行调试（比如使用 vscode）
- `deno task pkg`
  打包 electron 应用
- `deno task plaoc serve ./DIR_OR_URL`
  将制定目录或者链接作为应用启动

# TODO

- ipc
  1. 新增 `IpcError` 消息类型，该消息类型可以从本地发出、也可以通过接收得到
     - 用于对 `ipc.postMessage` 行为抛出异常，比如可以模拟 `network-error` 的行为，从而中断 `ipc.fetch`
  1. `IpcStreamMessage` 新增 `index` 属性
     - 使得接收端与发起段都可以更高并行地收发数据，从而提升性能减少延迟。
     - 但是，ipc 协议仍然不负责处理丢包行为，这需要底层数据通道自己解决可靠性问题
     - 我们会在缓冲区到达一定程度的时候，发出`IpcStreamPause`信息，如果对方不遵守（或者可能并发性问题，没有及时遵守暂停指令），接收端会发出 `IpcError`，告知对方缓冲区溢出，需要等收到 `IpcStreamPull` 的时候重发。
  1. `IpcMessage` 新增 `ipc.channelId[string]`与`ipc.parentIpc[Ipc?]` 属性、增加 `ipc.createChannel()`与`ipc.onChannel()` 函数。
     - 使得一个 ipc 可以分发出多个子 ipc，用于替代大多数场景下的 `ReadableStreamIpc`。因为`ReadableStreamIpc`是基于`IpcRequest`、`IpcResponse`内的`IpcBodyStream`构建出来的一个抽象`Ipc`。这并非完全属于`IpcBodyStream`的本意，只是 Ipc 协议的灵活性使得我们可以利用任意的流来构建出一个 ipc 实例。但是我们确实在很多场景下需要用到这种需求，比如说`http.std.dweb`中的请求路由。如果只是基于`ReadableStreamIpc`不可避免的会有一层编解码的消耗。因此我们需要在原有 ipc 链路上创建出一条新的 ipc 子链路，在父级关闭时子链路也会关闭。（这类似在 WebWorker 里创建 MessageChannel 一样，这些 MessageChannel 会随着 WebWorker 的关闭而关闭）
  1. 完善通讯协议编码，正式接入 cbor；实现协议互相的翻译
     - 数据传输一共有三种格式：结构化传输(Naitve)、String(JSON)、Binary(Cbor)。其中结构化传输是指编程语言自身支持的对象传输，通常来说它的开销最小，比如可能是直接的引用传递，或者像 js 环境中的结构化克隆；然后是 JSON-String，某些环境下只能传输字符串的情况下使用，比如 Android 与 WebView 交互只能传输 String，IOS 与 WKWebView 交互的最佳方案也是传输 String；再有就是 Cbor-Binary，在网络环境中，比如 WebSocket 或者 FetchRequest，都能支持 Binary 的传输，并且其针对 IpcMessage 的编解码性能不比 JSON 格式来的差，此时可以考虑优先使用 Cbor-Binary。
     - 两个模块在建立 Ipc 连接的时候，首先由 DNS 决策它们是否能直接使用 Native 传输，否则需要进行协商，优先使用 Binary，其次是 String
- http.sys.dweb
  1. `/start` 也可以携带 routes 配置。
     - 为了向下兼容，`/start` 默认监听所有的请求。
     - `/start` 的 routes 是为了做到权限风发，与`listen` 的 routes 为了做到负载均衡不一样，二者互相补充
- dwebview
  1. closewatcher.shim 应该直接垫片到每一个 WebView 实例中

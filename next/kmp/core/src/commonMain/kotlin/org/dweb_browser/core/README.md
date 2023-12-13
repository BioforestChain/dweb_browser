## 关于 IpcRequest 与 PureRequest 的关系

```mermaid
flowchart TD
    D -->|"toServer()"| C
    A[IpcClientRequest] -->|"toServer(ipc)"| B(IpcServerRequest)
    B -->|"toPure()"| C[PureServerRequest]
    C -->|"toClient()"| D[PureClientRequest]
    D -->|"toIpc(ipc)"| A
```

1. Pure系列是最顶层的抽象，一般来说直接面向它开始编程总是没错的。
2. 正如 nativeFetch 需要参数，请求是从 PureClientRequest 开始构建。底层会按需自动转化成
   ipcClientRequest
3. ipcClientRequest 被 clientIpc.postMessage 传输，被 serverIpc.onMessage 收到，此时该
   ipcClientRequest 会被转化成 ipcServerRequest，用于 onRequest{} 响应函数内。
4. 出于安全性，ipcServerRequest 不允许直接逆转为 ipcClientRequest。通常有这种需求的，是需要对请求进行转发，那么此时需要借用
   pure 系列来进行间接转化，比如：nativeFetch(ipcServerRequest.toPure().toClient())
5. PureServerRequest 作为最顶层的抽象，并不总是来自于 ipcServerRequest。比如：有可能来自 Ktor 的
   ApplicationRequest
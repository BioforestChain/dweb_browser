
 



enum Method {
    case GET // 查
    case POST // 增
    case PUT // 改：替换
    case PATCH // 改：局部更新
    case DELETE // 删
    case OPTIONS //  嗅探
    case HEAD // 预查
    case CONNECT // 双工
    case TRACE // 调试
}

/** TODO
1、types.cts -> RequestInit Response 类型怎么翻译
2、createSignal.cts -> Signal类的listen方法的return结果是什么
3、const.cts -> RawData是什么类型
4、IpcBody.cts -> stream() -> Blob.stream()
5、urlHelper.cts -> URL_BASE
6、streamAsRawData
7、IpcResponse -> asResponse -> 构建response
8、ipc.cts -> onMessage = this._messageSignal.listen 什么功能
9、PromiseOut
10、types.cts -> "mmid"
**/

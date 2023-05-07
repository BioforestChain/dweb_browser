# Pure-Http

这里提供的是中立的 Pure-Request/Response 的抽象定义，用于 NativeFetch
它可以以极低成本与 Ipc-Request/Reponse 进行转换，也可以转换成 C#官方的 HttpMessage

Pure 与 Ipc 的本质区别在于，Ipc-Request/Reponse 耦合了 ipc 连接的概念，Pure 没有，只是纯粹的无状态对象。

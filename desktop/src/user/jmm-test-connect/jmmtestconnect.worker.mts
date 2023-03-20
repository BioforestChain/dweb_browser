const main = async() => {
    const { IpcEvent } = ipc
    // 等待其他模块的主动连接
    {
        jsProcess.onConnect((remoteIpc) => {
            console.log('jsmtestconnect.worker.mts onConnect')
            remoteIpc.onMessage((message, ipc) => {
                console.log('jmtestconnect.worker.mct onmessage', message)

                setTimeout(() => {
                    // 返回消息
                    // 返回消息不需要有模块的名称
                    remoteIpc.postMessage(IpcEvent.fromText('返回的消息 key', "返回的消息value"))
                },1000)
            })
        })
    }
}

main()
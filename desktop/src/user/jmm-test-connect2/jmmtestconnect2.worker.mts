const main = async() => {
    const { IpcEvent } = ipc;
    // 等待其他模块的主动连接
    {
        jsProcess.onConnect((remoteIpc) => {
            console.log('jsmtestconnect-2.worker.mts onConnect')
            remoteIpc.onMessage((message, ipc) => {
                console.log('jmtestconnect-2.worker.mct onmessage', message)
            })
        })
    }

    // 主动连接其他模块
    {
        setTimeout(async () => {
            const jsMMIpc = await jsProcess.connect('jmm.test.connect.dweb')

            // jsMMIpc.onMessage((message, ipc) => {
            //     console.log('jmm.text.connect.2.worker.mts 接受到了消息')

            // })
            // name 必须是指定的模块名称
            // jsMMIpc.postMessage(IpcEvent.fromText('jmm.test.connect.dweb', "value"))

        }, 3000)
    }


}

main()
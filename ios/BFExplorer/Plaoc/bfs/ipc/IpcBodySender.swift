//
//  IpcBodySender.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation
import Atomics

/**
 * IpcBodySender 本质上是对 ReadableStream 的再次封装。
 * 我们知道 ReadableStream 本质上是由 stream 与 controller 组成。二者分别代表着 reader 与 writer 两个角色。
 *
 * 而 IpcBodySender 则是将 controller 给一个 ipc 来做写入，将 stream 给另一个 ipc 来做接收。
 * 而关键点就在于这两个 ipc 很可能不是对等关系
 *
 * 因为 IpcBodySender 会被 IpcRequest/http4kRequest、IpcResponse/http4kResponse 对象转换的时候传递，
 * 中间被很多个 ipc 所持有过，而每一个持有过它的人都有可能是这个 stream 的读取者。
 *
 * 因此我们定义了两个集合，一个是 ipc 的 usableIpcBodyMap；一个是 ipcBodySender 这边的 usedIpcMap
 *
 */
class IpcBodySender: IpcBody {
    typealias SimpleCallback = AsyncCallback<(), Any>
    
    private let ipc: Ipc
    
    init(raw: Any, ipc: Ipc) {
        self.ipc = ipc
        super.init()
        self.raw = raw
    }
    
    lazy var isStream: Bool = {
        raw is InputStream
    }()
    var isStreamClosed: Bool {
        get {
            isStream ? _isStreamClosed : true
        }
    }
    var isStreamOpened: Bool {
        get {
            isStream ? _isStreamOpened : true
        }
    }
    
    private let pullSignal = Signal<()>()
    private let abortSignal = Signal<()>()
    
    class IPC {
        struct UsableIpcBodyMapper {
            private var map: [String:IpcBodySender] = [:]
            
            mutating func add(streamId: String, ipcBody: IpcBodySender) -> Bool {
                if map.contains(where: { $0.key == streamId }) {
                    return false
                }
                
                self.map[streamId] = ipcBody
                
                return true
            }
            
            func get(streamId: String) -> IpcBodySender? {
                map[streamId]
            }
            
            mutating func remove(streamId: String) async -> IpcBodySender? {
                let result = self.map.removeValue(forKey: streamId)
                
                if self.map.isEmpty {
                    await self.destroySignal.emit(())
                    await self.destroySignal.clear()
                }
                
                return result
            }
            
            private let destroySignal = Signal<()>()
            
            func onDestroy(cb: @escaping SimpleCallback) -> Signal.OffListener {
                destroySignal.listen(cb)
            }
        }
        
        private var IpcUsableIpcBodyMap: [Ipc:UsableIpcBodyMapper] = [:]
        
        /**
         * ipc 将会使用 ipcBody
         * 那么只要这个 ipc 接收到 pull 指令，就意味着成为"使用者"，那么这个 ipcBody 都会开始读取数据出来发送
         * 在开始发送第一帧数据之前，其它 ipc 也可以通过 pull 指令来参与成为"使用者"
         */
        func usableByIpc(ipc: Ipc, ipcBody: IpcBodySender) {
            if ipcBody.isStream && !ipcBody._isStreamOpened {
                let streamId = ipcBody.metaBody.streamId!
                var usableIpcBodyMapper = IpcUsableIpcBodyMap[ipc]
                
                if usableIpcBodyMapper == nil {
                    usableIpcBodyMapper = UsableIpcBodyMapper()
                    
                    let off = ipc.onMessage { (message, _) in
                        if let message = message as? IpcStreamPull {
                            let ipcBody = usableIpcBodyMapper!.get(streamId: message.stream_id)
                            
                            if ipcBody != nil {
                                // 一个流一旦开启了，那么就无法再被外部使用了
                                if ipcBody!.useByIpc(ipc:ipc) {
                                    // ipc 将使用这个 body，也就是说接下来的 MessageData 也要通知一份给这个 ipc
                                    await ipcBody!.emitStreamPull(message: message, ipc: ipc)
                                }
                            }
                        } else if let message = message as? IpcStreamAbort {
                            // 一个流一旦开启了，那么就无法再被外部使用了
                            let ipcBody = usableIpcBodyMapper!.get(streamId: message.stream_id)
                            
                            if ipcBody != nil {
                                await ipcBody!.unuseByIpc(ipc: ipc)
                            }
                        }
                        
                        return nil
                    }
                    
                    _ = usableIpcBodyMapper?.onDestroy(cb: off)
                    _ = usableIpcBodyMapper?.onDestroy {
                        self.IpcUsableIpcBodyMap.removeValue(forKey: ipc)
                    }
                }
                
                if usableIpcBodyMapper!.add(streamId: streamId, ipcBody: ipcBody) {
                    // 一个流一旦关闭，那么就将不再会与它有主动通讯上的可能
                    _ = ipcBody.onStreamClose {
                        await usableIpcBodyMapper?.remove(streamId: streamId)
                    }
                }
            }
        }
    }
    
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    private var usedIpcMap: [Ipc:/* desiredSize */Int] = [:]
    
    /**
     * 绑定使用
     */
    private func useByIpc(ipc: Ipc) -> Bool {
        if usedIpcMap.contains(where: { $0.key == ipc }) {
            return true
        }
        
        if isStream && !_isStreamOpened {
            usedIpcMap[ipc] = 0
            _ = closeSignal.listen {
                await self.unuseByIpc(ipc: ipc)
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 拉取数据
     */
    private func emitStreamPull(message: IpcStreamPull, ipc: Ipc) async {
        let pulledSize = usedIpcMap[ipc]! + message.desiredSize
        usedIpcMap[ipc] = pulledSize
        await pullSignal.emit(())
    }
    
    /**
     * 解绑使用
     */
    private func unuseByIpc(ipc: Ipc) async {
        if usedIpcMap.removeValue(forKey: ipc) != nil {
            if usedIpcMap.isEmpty {
                await self.abortSignal.emit(())
            }
        }
    }
    
    private let closeSignal = Signal<()>()
    
    func onStreamClose(cb: @escaping SimpleCallback) -> Signal.OffListener {
        closeSignal.listen(cb)
    }
    
    private let openSignal = Signal<()>()
    
    func noStreamOpen(cb: @escaping SimpleCallback) -> Signal.OffListener {
        openSignal.listen(cb)
    }
    
    private var _isStreamOpenedDefault = false
    private var _isStreamOpened: Bool {
        get {
            _isStreamOpenedDefault
        }
        set {
            if _isStreamOpenedDefault != newValue {
                _isStreamOpenedDefault = newValue
                
                
            }
        }
    }
    
    private var _isStreamClosedDefault = false
    private var _isStreamClosed: Bool {
        get {
            _isStreamClosedDefault
        }
        set {
            if _isStreamClosedDefault != newValue {
                _isStreamClosedDefault = newValue
                
                
            }
        }
    }
    
    private func emitStreamClose() {
        _isStreamOpened = true
        _isStreamClosed = true
    }
    
    
    private func bodyAsMeta(body: Any, ipc: Ipc) -> MetaBody {
        if let body = body as? String {
            return MetaBody.fromText(senderUid: ipc.uid, data: body)
        } else if let body = body as? Data {
            return MetaBody.fromBinary(senderIpc: ipc, data: body)
        } else if let body = body as? InputStream {
            return streamAsMeta(stream: body, ipc: ipc)
        }
    }
    
    private func streamAsMeta(stream: InputStream, ipc: Ipc) -> MetaBody {
        let stream_id = IpcBodySender.getStreamId(stream: stream)
        
        let sendingLock = ManagedAtomic<Bool>(false)
        
        func sender() async {

        }
        
        _ = pullSignal.listen {
            await sender()
        }
        
        _ = abortSignal.listen {
            stream.close()
            self.emitStreamClose()
            
            return nil
        }
    }
    
    
    private static var streamIdWM: [(InputStream,String)] = []
    
    private static var stream_id_acc = 1
    static func getStreamId(stream: InputStream) -> String {
        if let i = streamIdWM.firstIndex(where: { $0.0 == stream }) {
            return streamIdWM[i].1
        } else {
            let stream_id = "rs-\(stream_id_acc++)"
            streamIdWM.append((stream, stream_id))
            return stream_id
        }
    }
}

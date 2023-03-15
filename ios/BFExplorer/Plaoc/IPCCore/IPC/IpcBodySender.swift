//
//  IpcBodySender.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

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

import UIKit


class IpcBodySender: IpcBody {
    
    private var streamIdWM: [InputStream: String] = [:]
    private var stream_id_acc = 1
    private let pullSignal = SimpleSignal()
    private let abortSignal = SimpleSignal()
    private let closeSignal = SimpleSignal()
    private let openSignal = SimpleSignal()
    private var usedIpcMap: [Ipc:Int] = [:]
    private var IpcUsableIpcBodyMap: [Ipc:UsableIpcBodyMapper] = [:]
    private let lock = NSLock()
    
    var isStream: Bool {
        return raw is InputStream
    }
    
    var isStreamClosed: Bool {
        return isStream ? _isStreamClosed : true
    }
    
    var isStreamOpened: Bool {
        return isStream ? _isStreamOpened : true
    }
    
    var _isStreamClosed: Bool = false {
        didSet {
            closeSignal.emit(())
            closeSignal.clear()
        }
    }
    
    var _isStreamOpened: Bool = false {
        didSet {
            openSignal.emit(())
            openSignal.clear()
        }
    }
    
    override init() {
        
    }
    
    init(raw: Any, ipc: Ipc) {
        super.init()
        self.raw = raw
        self.ipc = ipc
        self.bodyHub = hub
        self.metaBody = bodyAsMeta(body: raw, ipc: ipc)
        
        raw_ipcBody_WMap[raw] = self
        // 作为 "生产者"，第一持有这个 IpcBodySender
        IPCsender(ipc: ipc, ipcBody: self)
    }
    
    // 作为 "生产者"，第一持有这个 IpcBodySender
    func IPCsender(ipc: Ipc, ipcBody: IpcBodySender) {
        IPC().usableByIpc(ipc: ipc, ipcBody: ipcBody)
    }
    
    //绑定使用
    func useByIpc(ipc: Ipc) -> Bool {
        guard !usedIpcMap.keys.contains(ipc) else { return true }
        
        if isStream, !_isStreamOpened {
            usedIpcMap[ipc] = 0
            _ = closeSignal.listen { _ in
                self.unuseByIpc(ipc: ipc)
            }
            return true
        }
        return false
    }
    
    //拉取数据
    func emitStreamPull(message: IpcStreamPull, ipc: Ipc) {
        
        //desiredSize 仅作参考，我们以发过来的拉取次数为准
        let pullSize = usedIpcMap[ipc] ?? 0 + message.desiredSize
        usedIpcMap[ipc] = pullSize
        pullSignal.emit(())
    }
    //解绑使用
    func unuseByIpc(ipc: Ipc) {
        if usedIpcMap.removeValue(forKey: ipc) != nil {
            //如果没有任何消费者了，那么真正意义上触发 abort
            if usedIpcMap.isEmpty {
                abortSignal.emit(())
            }
        }
    }
    
    func onStreamClose(cb: @escaping SimpleCallbcak) -> OffListener {
        return closeSignal.listen(cb)
    }
    
    
    func onStreamOpen(cb: @escaping SimpleCallbcak) -> OffListener {
        return openSignal.listen(cb)
    }
    
    private func emitStreamClose() {
        _isStreamOpened = true
        _isStreamClosed = true
    }
    
    lazy private var hub: BodyHub = {
        var body = BodyHub()
        body.data = self.raw
        if let str = self.raw as? String {
            body.text = str
        } else if let bytes = self.raw as? [UInt8] {
            body.u8a = bytes
        } else if let stream = self.raw as? InputStream {
            body.stream = stream
        }
        return body
    }()
    
    static func from(raw: Any, ipc: Ipc) -> IpcBody {
        return raw_ipcBody_WMap[raw] as? IpcBody ?? IpcBodySender(raw: raw, ipc: ipc)

    }
    
    private func getStreamId(stream: InputStream) -> String {
        let tmp = stream_id_acc
        stream_id_acc += 1
        let idwm = streamIdWM[stream]
        if idwm == nil {
            if let readStream = stream as? ReadableStream {
                streamIdWM[readStream] = "rs-\(tmp)[\(readStream.uid)]"
            } else {
                streamIdWM[stream] = "rs-\(tmp)"
            }
        }
        return streamIdWM[stream]!
    }
    
    private func bodyAsMeta(body: Any, ipc: Ipc) -> MetaBody? {
        
        if let content = body as? String {
            return MetaBody.fromText(senderUid: ipc.uid, data: content)
        } else if let bytes = body as? [UInt8] {
            return MetaBody.fromBinary(senderIpc: ipc, data: bytes)
        } else if let stream = body as? InputStream {
            return streamAsMeta(stream: stream, ipc: ipc)
        }
        return nil
    }
    
    private func streamAsMeta(stream: InputStream, ipc: Ipc) -> MetaBody {
        let stream_id = getStreamId(stream: stream)
        
        _ = pullSignal.listen { _ in
            Task {
                self.sender(stream: stream, stream_id: stream_id)
            }
        }
        
        _ = abortSignal.listen { _ in
            stream.close()
            self.emitStreamClose()
            return -1 //随便返回数据
        }
        
        
        var streamType = IPC_META_BODY_TYPE.IPC_META_BODY_TYPE_STREAM_ID
        var streamFirstData: Any
        if let preStream = stream as? PreReadableInputStream, preStream.preReadableSize > 0 {
            streamFirstData = stream.readByteArray(size: preStream.preReadableSize)
            streamType = .STREAM_WITH_BINARY
        } else {
            streamFirstData = ""
        }
        
        let meta = MetaBody(type: streamType, senderUid: ipc.uid, data: streamFirstData, streamId: stream_id, receiverUid: nil)
        metaId_ipcBodySender_Map[meta.metaId] = self
        _ = abortSignal.listen { _ in
            metaId_ipcBodySender_Map.removeValue(forKey: meta.metaId)
        }
        return meta
    }
    
    private func sender(stream: InputStream,stream_id: String) {
        
        if lock.try() {
            
            stream.open()
            defer {
                stream.close()
            }
            
            let bufferSize = 1024
            let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
            defer {
                buffer.deallocate()
            }
            
            var emptyByte: Bool = true
            while stream.hasBytesAvailable {
                emptyByte = false
                let leng = stream.read(buffer, maxLength: bufferSize)
                let data = Data(bytes: buffer, count: leng)
                if leng <= 0 {
                    emptyByte = true
                    break
                } else {
                    _isStreamOpened = true
                    let message = IpcStreamData.fromBinary(stream_id: stream_id, data: [UInt8](data))
                    
                    
                    for (ipc, _) in usedIpcMap {
                        ipc.postMessage(message: message)
                    }
                }
                emptyByte = true
            }
            
            if emptyByte {
                finishStream(stream_id: stream_id)
            }
            
            lock.unlock()
        }
    }
    
    private func finishStream(stream_id: String) {
        let message = IpcStreamEnd(stream_id: stream_id)
        for (ipc, _) in usedIpcMap {
            ipc.postMessage(message: message)
        }
    }
}


class IPC {
    
    private var IpcUsableIpcBodyMap: [Ipc:UsableIpcBodyMapper] = [:]
    
    func usableByIpc(ipc: Ipc, ipcBody: IpcBodySender) {
        
        guard ipcBody.isStream, !ipcBody._isStreamOpened else { return }
        
        let streamId = ipcBody.metaBody?.streamId ?? ""
        var mapper = IpcUsableIpcBodyMap[ipc]
        if mapper == nil {
            mapper = UsableIpcBodyMapper()
            let closure = ipc.onMessage(cb: { (message, _) in
                if let pull = message as? IpcStreamPull {
                    let bodySender = mapper?.get(streamId: pull.stream_id ?? "")
                    if bodySender != nil {
                        if bodySender!.useByIpc(ipc: ipc) {
                            bodySender?.emitStreamPull(message: pull, ipc: ipc)
                        }
                    }
                }
                if let abort = message as? IpcStreamAbort {
                    let bodySender = mapper!.get(streamId: abort.stream_id ?? "")
                    if bodySender != nil {
                        ipcBody.unuseByIpc(ipc: ipc)
                    }
                }
            })
            _ = mapper?.onDestroy(cb: closure)
            _ = mapper?.onDestroy(cb: { _ in
                self.IpcUsableIpcBodyMap.removeValue(forKey: ipc)
            })
        }
        IpcUsableIpcBodyMap[ipc] = mapper
        
        if mapper!.add(streamId: streamId, ipcBody: ipcBody) {
            _ = ipcBody.onStreamClose { _ in
                mapper!.remove(streamId: streamId)
            }
        }
    }
}

protocol PreReadableInputStream {
    
    var preReadableSize: Int { get }
}

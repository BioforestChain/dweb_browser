//
//  IpcBodySender.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/27.
//

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
   
    
//    override var raw: Any
    /**
         * 当前收到拉取的请求数
         */
    private var curPulledTimes = 0;
    
    var isStream: Bool {
        return raw is InputStream
    }
    
    var isStreamClosed: Bool {
        if isStream {
            return _isStreamClosed
        }
        return true
    }
    
    var isStreamOpened: Bool {
        if isStream {
            return _isStreamOpened
        }
        return true
    }
    
    private var _isStreamClosed: Bool = false {
        didSet {
            closeSignal.emit(())
            closeSignal.clear()
        }
    }
    
    private var _isStreamOpened: Bool = false {
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
        
        wm[self] = raw
        
        usableByIpc(ipc: ipc, ipcBody: self)
    }
    
    //绑定使用
    private func useByIpc(ipc: Ipc) -> Bool {
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
    private func emitStreamPull(message: IpcStreamPull, ipc: Ipc) {
        
        //desiredSize 仅作参考，我们以发过来的拉取次数为准
        let pullSize = usedIpcMap[ipc] ?? 0 + message.desiredSize
        usedIpcMap[ipc] = pullSize
        pullSignal.emit(())
    }
    //解绑使用
    private func unuseByIpc(ipc: Ipc) {
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
    
    func usableByIpc(ipc: Ipc, ipcBody: IpcBodySender) {
        
        guard ipcBody.isStream, !ipcBody._isStreamOpened else { return }
        
        let streamId = ipcBody.metaBody?.data as? String ?? ""
        var mapper = IpcUsableIpcBodyMap[ipc]
        if mapper == nil {
            mapper = UsableIpcBodyMapper()
            let closure = ipc.onMessage({ (message, _) in
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
                return ipc.messageSignal?.closure
            })
            _ = mapper?.onDestroy(cb: closure)
            _ = mapper?.onDestroy(cb: { _ in
                self.IpcUsableIpcBodyMap.removeValue(forKey: ipc)
            })
        }
        
        if mapper!.add(streamId: streamId, ipcBody: ipcBody) {
            _ = ipcBody.onStreamClose { _ in
                mapper!.remove(streamId: streamId)
            }
        }
    }
    
    func from(raw: Any, ipc: Ipc) -> IpcBodySender {
        
        var sender: IpcBodySender?
        for (key, value) in wm {
            if Tools.equals(value, raw) {
                sender = key as? IpcBodySender
                break
            }
        }
        if sender == nil {
            sender = IpcBodySender(raw: raw, ipc: ipc)
        }
        return sender!
    }
    
    private func getStreamId(stream: InputStream) -> String {
        let tmp = stream_id_acc
        stream_id_acc += 1
        let idwm = streamIdWM[stream]
        if idwm == nil {
            streamIdWM[stream] = "rs-\(tmp)"
        }
        return streamIdWM[stream]!
    }
    
    lazy private var hub: Body = {
        var body = Body()
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
    
    
    
    private func bodyAsMeta(body: Any, ipc: Ipc) -> MetaBody? {
        
        if let content = body as? String {
            return textAsMeta(text: content, ipc: ipc)
        } else if let bytes = body as? [UInt8] {
            return binaryAsMeta(binary: bytes, ipc: ipc)
        } else if let stream = body as? InputStream {
            return streamAsMeta(stream: stream, ipc: ipc)
        }
        return nil
    }
    
    private func textAsMeta(text: String, ipc: Ipc) -> MetaBody {
        return MetaBody(type: .TEXT, data: text, ipcUid: ipc.uid)
    }
    
    private func binaryAsMeta(binary: [UInt8], ipc: Ipc) -> MetaBody {
        if ipc.support_binary {
            return MetaBody(type: .BINARY, data: binary, ipcUid: ipc.uid)
        }
        return MetaBody(type: .BASE64, data: encoding.simpleDecoder(data: binary, encoding: .base64), ipcUid: ipc.uid)
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
        }
        
        return MetaBody(type: .STREAM_ID, data: stream_id, ipcUid: ipc.uid)
    }
    
    private func sender(stream: InputStream,stream_id: String) {
        
        let result = curPulledTimes
        curPulledTimes += 1
        
        guard result > 0 else { return }
        
        while curPulledTimes > 0 {
            
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
                if leng <= 0 {
                    emptyByte = true
                    break
                } else {
                    _isStreamOpened = true
                    let data = Data(bytes: buffer, count: leng)
                    
                    for (ipc, _) in usedIpcMap {
                        if ipc.support_binary {
                            ipc.postMessage(message: IpcStreamData.asBinary(stream_id: stream_id, data: [UInt8](data)))
                        } else {
                            ipc.postMessage(message: IpcStreamData.asBase64(stream_id: stream_id, data: [UInt8](data)))
                        }
                        
                    }
                }
                emptyByte = true
            }
            
            if emptyByte {
                finishStream(stream_id: stream_id)
            }
            
            curPulledTimes = 0
        }
    }
    
    private func finishStream(stream_id: String) {
        let message = IpcStreamEnd(stream_id: stream_id)
        for (ipc, _) in usedIpcMap {
            ipc.postMessage(message: message)
        }
        emitStreamClose()
    }
}

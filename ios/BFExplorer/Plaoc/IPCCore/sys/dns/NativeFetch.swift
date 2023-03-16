//
//  NativeFetch.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/1.
//

import UIKit
import Vapor
import MobileCoreServices


typealias FetchAdapter = (_ remote: MicroModule, _ request: Request) -> Response?

let nativeFetchAdaptersManager = AdapterManager<FetchAdapter>()


class ChunkAssetsFileStream: InputStream, PreReadableInputStream {

    static let defaultChunkSize = 1024 * 1024
    
    var source: InputStream?
    var chunkSize: Int?
    var preReadableSize: Int = 0
    
    var ptr = 0
    var totalSize: Int = 0
    
    init(source: InputStream, chunkSize: Int = defaultChunkSize, preReadableSize: Int?) {
        
        super.init(data: Data())
        self.source = source
        self.chunkSize = chunkSize
        self.preReadableSize = preReadableSize ?? chunkSize
        
        totalSize = source.available()
    }
    
}

func localeFileFetch(remote: MicroModule, request: Request) -> Response? {
    
    if request.url?.scheme == "file", request.url?.host == "" {
        
    }
}

class NativeFetch {
    
    //加载本地文件
    static func localeFileFetch(remote: MicroModule, request: Request) -> Response? {
        
        if request.url.scheme == "file", request.url.host == "", let url = URL(string: request.url.string) {
            
            let path = url.pathComponents.joined(separator: "/")
            return request.fileio.streamFile(at: Bundle.main.bundlePath + "/app/sdk\(path)")
            /*
            do {
                var path = request.url?.path ?? ""
                if path.contains("/") {
                    path.remove(at: path.startIndex)
                }
                let bodyContent = try FileSystemManager.readFile(at: URL.init(fileURLWithPath: path), with: true)
                var headers = HTTPHeaders()
                headers.add(name: "Content-Type", value: NativeFetch.mimeType(pathExtension: path))
                let body = Response.Body.init(string: bodyContent)
                let response = Response(status: .ok, headers: headers, body: body)
                return response
            } catch {
                let content = "the \(request.url?.path) file not found."
                let body = Response.Body.init(string: content)
                let response = Response(status: .notFound, headers: HTTPHeaders(), body: body)
                return response
            }*/
        }
        return nil
    }
    
    
    static func mimeType(pathExtension: String) -> String {
        
        let defaultMIMEType = "application/octet-stream"
        // 获取⽂件名后缀标记
        guard let tag = pathExtension.components(separatedBy: "/").last?
            .components(separatedBy: ".").last?
            .trimmingCharacters(in: .whitespacesAndNewlines) else { return defaultMIMEType }
        // 异常则返回⼆进制通⽤类型
        guard let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, tag as CFString, nil)?.takeRetainedValue(),
              let mimeType = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue()
        else { return defaultMIMEType }
        return mimeType as String
    }
    
   
}




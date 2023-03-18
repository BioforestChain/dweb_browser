//
//  ServerUrlInfo.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/3.
//

import UIKit
import Vapor

struct ServerUrlInfo {

    //标准host，是一个站点的key，只要站点过来时用某种我们认可的方式（x-host/user-agent）携带了这个信息，那么我们就依次作为进行网关路由
    var host: String
    //内部链接，带有特殊的协议头，方便自定义解析器对其进行加工
    var internal_origin: String
    //相对公网的链接（这里只是相对标准网络访问，当然目前本地只支持localhost链接，所以这里只是针对webview来使用）
    var public_origin: String
    
    init(host: String, internal_origin: String, public_origin: String) {
        self.host = host
        self.internal_origin = internal_origin
        self.public_origin = public_origin
    }
    
    func buildPublicUrl() -> URL? {
        return URL(string: public_origin)!.addURLQuery(name: "X-DWeb-Host", value: host)
    }
    
    func buildInternalUrl() -> URL? {
        return URL(string: internal_origin)
    }
}

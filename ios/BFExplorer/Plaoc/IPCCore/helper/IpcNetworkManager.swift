//
//  IpcNetworkManager.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/24.
//

import UIKit
import UIKit
import PromiseKit
import Vapor

class IpcNetworkManager: NSObject {

    enum NetworkError: Error {
        case urlError(msg: String)
        
    }

    private(set) var mmid: String = ""
    
    func fetch(input: Any) -> Promise<Response> {
        
        return Promise(resolver: { (resolver) in
            let success = { (result) -> Void in
                resolver.fulfill(result)
            }
            let failure = {(result: Error) -> Void in
                resolver.reject(result)
            }
            
            var url: URL?
            if input is String {
                url = URL(string: input as? String ?? "")
            }
            
            if input is URL {
                url = input as? URL
            }
            
            guard url != nil else { return failure(NetworkError.urlError(msg: "url error")) }
            let request = URLRequest(url: url!)
            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                if error != nil {
                    failure(error!)
                } else {
                    if data != nil {
                        if let response = response as? HTTPURLResponse {
                            var headers = HTTPHeaders()
                            let headerFields = response.allHeaderFields as? [String:String]
                            if headerFields != nil {
                                for (key, value) in headerFields! {
                                    headers.add(name: key, value: value)
                                }
                            }
                            
                            let res = Response(status: HTTPResponseStatus(statusCode: response.statusCode), headers: headers, body: Response.Body.init(data: data!))
                            success(res)
                        }
                    }
                }
            }
            task.resume()
        })
    }
}

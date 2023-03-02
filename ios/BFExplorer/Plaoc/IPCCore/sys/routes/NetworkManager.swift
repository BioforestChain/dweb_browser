//
//  NetworkManager.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import UIKit
import Vapor

class NetworkManager: NSObject {

    static func downLoadBody(url: URL, method: String) -> Response? {
        var request = URLRequest(url: url)
        request.httpMethod = method
        let response = NetworkManager.downLoadBodyByRequest(request: request)
        return response
    }
    
    static func downLoadBody(urlstring: String, method: String) -> Response? {
        guard let url = URL(string: urlstring) else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = method
        let response = NetworkManager.downLoadBodyByRequest(request: request)
        return response
    }
    
    static func downLoadBodyByRequest(request: URLRequest) -> Response? {
        
        let semaphore = DispatchSemaphore(value: 0)
        let config = URLSessionConfiguration.default
        let session = URLSession(configuration: config)
        var response: Response?
        let task = session.dataTask(with: request) { data, res, error in
            guard data != nil else {
                semaphore.signal()
                return
            }
            if let httpResponse = res as? HTTPURLResponse {
                let statusCode = httpResponse.statusCode
                var headers = HTTPHeaders()
                if let headerDict = httpResponse.allHeaderFields as? [String:String] {
                    for (key,value) in headerDict {
                        headers.add(name: key, value: value)
                    }
                }
                let responseBody = Response.Body.init(data: data!)
                response = Response(status: HTTPResponseStatus(statusCode: statusCode), headers: headers, body: responseBody)
            }
            semaphore.signal()
        }
        task.resume()
        
        semaphore.wait()
        return response
    }
}

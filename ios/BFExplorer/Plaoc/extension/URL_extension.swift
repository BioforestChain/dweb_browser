//
//  URL_extension.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/2.
//

import Foundation
import UniformTypeIdentifiers

extension URL {
    var urlParameters: [String: String]? {
        guard let components = URLComponents(url: self, resolvingAgainstBaseURL: true),
        let queryItems = components.queryItems else { return nil }
        return queryItems.reduce(into: [String: String]()) { (result, item) in
            result[item.name] = item.value
        }
    }
    
    func mimeType() -> String {
        if let mimeType = UTType(filenameExtension: self.pathExtension)?.preferredMIMEType {
            return mimeType
        } else {
            return "application/octet-stream"
        }
    }
    
    /// 替换当前URL的path
    func replacePath(_ path: String) -> URL {
        URL(string: self.path.replacingOccurrences(of: self.path, with: path))!
    }
    
    /// query追加
    func appending(_ queryItem: String, value: String?) -> URL {
        guard var urlComponents = URLComponents(string: absoluteString) else { return absoluteURL }

        // Create array of existing query items
        var queryItems: [URLQueryItem] = urlComponents.queryItems ??  []

        // Create query item
        let queryItem = URLQueryItem(name: queryItem, value: value)

        // Append the new query item in the existing query items array
        queryItems.append(queryItem)

        // Append updated query items array in the url component object
        urlComponents.queryItems = queryItems

        // Returns the url from new url components
        return urlComponents.url!
    }
    
    /**
     *  example:
     *  http://user:password@www.contoso.com:80/index.htm
     *  @return user:password@www.contoso.com:80
     */
    func authority() -> String {
        var prefix = ""
        if self.user != nil && self.password != nil {
            prefix += "\(self.user!):\(self.password!)@"
        }
        
        if self.host != nil {
            prefix += self.host!
        }
        
        if self.port != nil {
            prefix += ":\(self.port!)"
        }
        
        return prefix
    }
}

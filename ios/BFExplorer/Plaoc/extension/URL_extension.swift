//
//  URL_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation

extension URL {
    
    var urlParameters: [String:String]? {
        
        guard let components = URLComponents(url: self, resolvingAgainstBaseURL: true), let queryItems = components.queryItems else { return nil }
        return queryItems.reduce(into: [String:String]()) { result, item in
            result[item.name] = item.value
        }
    }
    
    //替换path
    func replacePath(replacePath: String) -> URL? {
        
        var components = URLComponents(url: self, resolvingAgainstBaseURL: true)
        components?.path = replacePath
        return components?.url
    }
    
    func addURLQuery(name: String, value: String?)  -> URL? {
        
        guard value != nil, value!.count > 0 else { return nil }
        guard var components = URLComponents(url: self, resolvingAgainstBaseURL: true), var queryItems = components.queryItems else { return nil }
        let item = URLQueryItem(name: name, value: value)
        queryItems.append(item)
        components.queryItems = queryItems
        return components.url
    }
    
    func authority() -> String {
        var prefix = ""
        if self.user != nil {
            prefix += self.user!
        }
        
        if self.password != nil {
            if prefix.count > 0 {
                prefix += ":\(self.password!)@"
            } else {
                prefix = "\(self.password!)@"
            }
        }
        
        if self.host != nil {
            prefix += self.host!
        }
        
        if self.port != nil {
            prefix += ":\(self.port!)"
        }
        
        return prefix
    }
    
    func getFullAuthority(hostOrAuthority: String? = nil) -> String {
        
        var authority = hostOrAuthority ?? authority()
        if !authority.contains(":") {
            if scheme == "http" {
                authority += ":80"
            } else if scheme == "https" {
                authority += "443"
            }
        }
        return authority
    }
}

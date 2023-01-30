//
//  ClipboardManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/19.
//

import UIKit

class ClipboardManager: NSObject {

    enum ContentType: String {
        case string
        case url
        case image
        case color
    }
    
    enum ClipboardError: LocalizedError {
        case invalidURL, invalidImage, invalidColor, invalidString, invalidType
        
        public var errorDescription: String? {
            switch self {
            case .invalidURL:
                return "Unable to form URL"
            case .invalidImage:
                return "Unable to encode Image"
            case .invalidColor:
                return "Unable to form Color"
            case .invalidString:
                return "Unable to form String"
            case .invalidType:
                return "Unfound type"
            }
        }
    }
    
    static func write(content: Any, ofType type: ContentType) ->Result<Void, Error> {
        switch type {
        case .string:
            if content is String {
                UIPasteboard.general.string = content as? String
                return .success(())
            } else {
                return .failure(ClipboardError.invalidString)
            }
        case .url:
            guard let content = content as? String else { return .failure(ClipboardError.invalidURL) }
            
            UIPasteboard.general.url = URL(string: content)
            return .success(())
        case .image:
            guard let content = content as? String else { return .failure(ClipboardError.invalidImage) }
            guard let stringData = Data(base64Encoded: content),
                  let uiImage = UIImage(data: stringData) else {
                return .failure(ClipboardError.invalidImage)
            }
            
            UIPasteboard.general.image = uiImage
            return .success(())
        case .color:
            guard let content = content as? String else { return .failure(ClipboardError.invalidColor) }
            let color = UIColor(hexColor: content)
            UIPasteboard.general.color = color
            
            return .success(())
        }
    }
    
    static func read() -> [String:Any] {
        if let stringValue = UIPasteboard.general.string {
            return [
                "value": stringValue,
                "type": "text/plain"
            ]
        }

        if let url = UIPasteboard.general.url {
            return [
                "value": url.absoluteString,
                "type": "text/plain"
            ]
        }

        if let image = UIPasteboard.general.image {
            let data = image.pngData()
            if let base64 = data?.base64EncodedString() {
                return [
                    "value": "data:image/png;base64," + base64,
                    "type": "image/png"
                ]
            }
        }
        
        if let color = UIPasteboard.general.color {
            return [
                "value": color.hexString(true),
                "type": "text/plain"
            ]
        }

        return [:]
    }
}


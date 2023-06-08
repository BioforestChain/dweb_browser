//
//  Publisher_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/9.
//

import Combine
import UIKit

extension Publishers {
    
    static var keyboardHeight: AnyPublisher<CGFloat, Never> {
        
        let willShow = NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)
            .map { $0.keyboardHeight }
        let willHiden = NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)
            .map { _ in CGFloat(0) }
        return MergeMany(willShow, willHiden).eraseToAnyPublisher()
    }
}

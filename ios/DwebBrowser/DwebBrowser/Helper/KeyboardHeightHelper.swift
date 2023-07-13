//
//  KeyboardHeightHelper.swift
//  DwebBrowser
//
//  Created by ui03 on 2023/7/11.
//

import SwiftUI

class KeyboardHeightHelper: ObservableObject {
    @Published var keyboardHeight: CGFloat = 0

    private func listenForKeyboardNotifications() {
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: OperationQueue.main) { noti in
            guard let value = noti.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            let height = value.height
            self.keyboardHeight = height - safeAreaBottomHeight
        }

        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { _ in
            self.keyboardHeight = 0
        }
    }

    init() {
        #if DwebFramework
            listenForKeyboardNotifications()
        #endif
    }
}

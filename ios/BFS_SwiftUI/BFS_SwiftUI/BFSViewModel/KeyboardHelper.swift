//
//  KeyboardHelper.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/9.
//

import SwiftUI

class KeyboardHelper: ObservableObject {
    
    @Published var keyboardHeight: CGFloat = 0
    
    
    private func listenForKeyboardNotification() {
        
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { noti in
            guard let userInfo = noti.userInfo,
                  let keyboardRect = userInfo[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            self.keyboardHeight = keyboardRect.height
        }
        
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { noti in
            self.keyboardHeight = 0
            
        }
    }
}


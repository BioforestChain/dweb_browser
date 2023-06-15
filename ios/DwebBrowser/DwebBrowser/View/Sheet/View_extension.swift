//
//  View_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

extension View {
    func dismissKeyboard() -> some View {
        return modifier(ResignKeyboardOnDragGesture())
    }
}

struct ResignKeyboardOnDragGesture: ViewModifier {
    
    var gesture = DragGesture().onChanged { _ in
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
    
    func body(content: Content) -> some View {
        content.gesture(gesture)
    }
}

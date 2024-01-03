//
//  CustomUIScrollView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI


struct CustomUIScrollView<Content: View>: View {
    
    private var content: Content
    @Binding var offset: CGPoint
    private var showIndication: Bool
    private var axis: Axis.Set
    @State var startOffset: CGPoint = .zero
    
    init(@ViewBuilder content: () -> Content, offset: Binding<CGPoint>, showIndication: Bool, axis: Axis.Set) {
        self.content = content()
        self._offset = offset
        self.showIndication = showIndication
        self.axis = axis
    }
    
    var body: some View {
        
        ScrollView(axis, showsIndicators: showIndication) {
            content
                .overlay(
                    GeometryReader(content: { proxy -> SwiftUI.Color in
                        let rect = proxy.frame(in: .global)
                        if startOffset == .zero {
                            DispatchQueue.main.async {
                                startOffset = CGPoint(x: rect.minX, y: rect.minY)
                            }
                        }
                        DispatchQueue.main.async {
                            self.offset = CGPoint(x: startOffset.x - rect.minX, y: startOffset.y - rect.minY)
                        }
                        return SwiftUI.Color.clear
                    })
                )
        }
    }
}

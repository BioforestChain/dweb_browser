//
//  CostomSheetView.swift
//  ResizeWindow
//
//  Created by ui06 on 9/1/23.
//

import SwiftUI

internal struct sheetYOffsetModifier<SheetView>: ViewModifier where SheetView: View {
    @Binding var isPresented: Bool

    @State private var startOffsetY: CGFloat = 0
    @State private var curDragOffsetY: CGFloat = 0
    @State private var endingOffsetY: CGFloat = 0
    var sheetView: SheetView
    func body(content: Content) -> some View {
        ZStack {
            GeometryReader { geo in
                let wndHeight = geo.frame(in: .local).height
                let sheetHeight = geo.frame(in: .local).height * 0.9
                content
                    .onAppear {
                        startOffsetY = wndHeight + toolAreaHeight
                    }
                    .onChange(of: geo.frame(in: .local).height, perform: { newValue in
                        startOffsetY = newValue + toolAreaHeight
                    })
                    .overlay {
                        sheetView
                            .padding(.horizontal, 4)
                            .padding(.vertical, 12)
                            .cornerRadius(10)
                            .offset(y: startOffsetY)
                            .offset(y: curDragOffsetY)
                            .offset(y: endingOffsetY)
                            .frame(height: sheetHeight)

                            .onChange(of: isPresented, perform: { _ in
                                if isPresented {
                                    withAnimation(.spring()) {
                                        startOffsetY = sheetHeight * 0.4
                                    }
                                }
                            })
                            .gesture(
                                DragGesture()
                                    .onChanged { value in
                                        let total = startOffsetY + curDragOffsetY + endingOffsetY
                                        if total == startOffsetY * 0.1, value.translation.height < 0 {
                                            curDragOffsetY = 0
                                        } else {
                                            withAnimation(.spring()) {
                                                curDragOffsetY = value.translation.height
                                            }
                                        }
                                    }
                                    .onEnded { _ in
                                        withAnimation(.spring()) {
                                            let total = startOffsetY + curDragOffsetY + endingOffsetY
                                            if curDragOffsetY < -50 {
                                                endingOffsetY = -startOffsetY * 0.9
                                            
                                            } else if curDragOffsetY > 50 {
                                                if startOffsetY == total {
                                                    startOffsetY = sheetHeight * 0.4
                                                } else {
                                                    startOffsetY = wndHeight + toolAreaHeight
                                                }
                                                endingOffsetY = 0
                                                isPresented = false
                                            }
                                            curDragOffsetY = 0
                                        }
                                    })
                    }
            }
            .background(.purple)
        }
    }
}

extension View {
    func resizableSheet<SheetView: View>(isPresented: Binding<Bool>, content: @escaping () -> SheetView) -> some View {
        modifier(sheetYOffsetModifier(isPresented: isPresented, sheetView: content()))
    }
}

struct MySheetView: View{
    var body: some View{
        ZStack {
            VStack {
                Text("this is sheet view")
                Spacer()

                HStack {
                    Text("leading bottom")
                    Spacer()
                    Text("trealing bottom")
                }
            }
        }
        .background(.green)
    }
}


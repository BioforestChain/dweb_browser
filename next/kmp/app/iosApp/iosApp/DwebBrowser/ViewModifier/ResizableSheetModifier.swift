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
    var sheetView: SheetView
    func body(content: Content) -> some View {
        ZStack {
            GeometryReader { geo in
                let wndHeight = geo.frame(in: .local).height
                let sheetHeight = wndHeight * 0.96
                content
                    .onAppear {
                        startOffsetY = wndHeight
                    }
                    .overlay {
                        sheetView
                            .frame(height: sheetHeight)
                            .cornerRadius(10)
                            .padding(.horizontal, 4)
                            .offset(y: startOffsetY)
                            .offset(y: curDragOffsetY)

                            .onChange(of: isPresented, perform: { _ in
                                if isPresented {
                                    withAnimation(.spring()) {
                                        startOffsetY = 0
                                    }
                                }
                            })
                            .gesture(
                                DragGesture()
                                    .onChanged { value in
                                        if value.startLocation.y < 30 {
                                            if value.translation.height < 0 {
                                                curDragOffsetY = 0
                                            } else {
                                                withAnimation(.spring()) {
                                                    curDragOffsetY = value.translation.height
                                                }
                                            }
                                        }
                                    }
                                    .onEnded { _ in
                                        withAnimation(.spring()) {
                                            if curDragOffsetY > 50 {
                                                startOffsetY = wndHeight
                                                isPresented = false
                                            }
                                            curDragOffsetY = 0
                                        }
                                    })
                    }
            }
        }
    }
}

extension View {
    func resizableSheet<SheetView: View>(isPresented: Binding<Bool>, content: @escaping () -> SheetView) -> some View {
        modifier(sheetYOffsetModifier(isPresented: isPresented, sheetView: content()))
    }
}

struct MySheetView: View {
    var body: some View {
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

//
//  CostomSheetView.swift
//  ResizeWindow
//
//  Created by ui06 on 9/1/23.
//

import SwiftUI

internal struct sheetYOffsetModifier<SheetView>: ViewModifier where SheetView: View {
    @Bindable var isPresented: ResizeSheetState

    @State private var startOffsetY: CGFloat = 0
    @State private var curDragOffsetY: CGFloat = 0
    @State private var sheetHeight: CGFloat = 0

    var sheetView: SheetView
    func body(content: Content) -> some View {
        ZStack {
            GeometryReader { geo in
                let wndHeight = geo.frame(in: .local).height
                let wndWidth = geo.frame(in: .local).width
                content
                    .onAppear {
                        sheetHeight = wndHeight * 0.98
                        startOffsetY = wndHeight
                    }
                    .overlay {
                        sheetView
                            .frame(height: sheetHeight)
                            .background(.blue)
                            .overlay {
                                Rectangle()
                                    .fill(Color.white.opacity(0.01))
                                    .frame(height: 30)
                                    .position(x: wndWidth / 2, y: 15)
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
                                                        isPresented.presenting = false
                                                    }
                                                    curDragOffsetY = 0
                                                }
                                            }
                                    )
                                    .accessibilityElement()
                                    .accessibilityIdentifier("moreHandle")
                            }
                            .cornerRadius(10)
                            .padding(.horizontal, 4)
                            .offset(y: startOffsetY)
                            .offset(y: curDragOffsetY)
                            .onChange(of: isPresented.presenting) { _, _ in
                                if isPresented.presenting {
                                    withAnimation(.spring()) {
                                        startOffsetY = 0
                                    }
                                } else {
                                    startOffsetY = wndHeight
                                }
                            }
                            .onChange(of: geo.size.height) { oldHeight, newHeight in
                                sheetHeight = newHeight * 0.98
                                startOffsetY = oldHeight == startOffsetY ? newHeight : 0
                            }
                    }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("PickerContainer")
    }
}

extension View {
    func resizableSheet<SheetView: View>(isPresented: ResizeSheetState, content: @escaping () -> SheetView) -> some View {
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

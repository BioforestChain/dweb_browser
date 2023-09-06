//
//  CostomSheetView.swift
//  ResizeWindow
//
//  Created by ui06 on 9/1/23.
//

import SwiftUI
let maxHeight = 500.0

internal struct DragModifier<SheetView>: ViewModifier where SheetView: View {
    @State private var startOffsetY: CGFloat = maxHeight
    @State private var curDragOffsetY: CGFloat = 0
    @State private var endingOffsetY: CGFloat = 0
    @Binding var isPresented: Bool
    var sheetView: SheetView
    func body(content: Content) -> some View {
        ZStack {
            content
            ZStack {
                Color(white: 0.8)
                VStack {
                    RoundedRectangle(cornerRadius: 5)
                        .frame(width: 80, height: 8)
                        .foregroundColor(.black.opacity(0.8))
                    
                    sheetView.padding(.bottom, maxHeight * 0.1)
                }
                .padding(.top)
            }
            .background(.cyan)
            .cornerRadius(20)
            .ignoresSafeArea(edges: .bottom)
            .offset(y: startOffsetY)
            .offset(y: curDragOffsetY)
            .offset(y: endingOffsetY)

            .onChange(of: isPresented, perform: { _ in
                if isPresented {
                    withAnimation(.spring()) {
                        startOffsetY = maxHeight * 0.4
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
                                    startOffsetY = maxHeight * 0.4
                                } else {
                                    startOffsetY = maxHeight
                                }
                                endingOffsetY = 0
                                isPresented = false
                            }
                            curDragOffsetY = 0
                        }
                    })
        }
    }
}

extension View {
    func resizableSheet<SheetView: View>(isPresented: Binding<Bool>, content: @escaping () -> SheetView) -> some View {
        modifier(DragModifier(isPresented: isPresented, sheetView: content()))
    }
}

struct PresentView: View {
    var body: some View {
        VStack {
            Text("This is the top of Book view")
                .padding()
                .background(.red)
            Spacer()
            Text("This is the bottom of Book view")
                .padding()
                .background(.red)
        }
        .background(.orange)
    }
}

struct SheetDemoView: View {
    @State private var isPresented = false

    var body: some View {
        ZStack {
            Color.green
            Button("Present Sheet") {
                isPresented = true
            }
            .resizableSheet(isPresented: $isPresented) {
                PresentView()
            }
        }
        .clipped()
        .frame(width: 300, height: maxHeight)
    }
}

struct SheetModifier_Previews: PreviewProvider {
    static var previews: some View {
        SheetDemoView()
    }
}

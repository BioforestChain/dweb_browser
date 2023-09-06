//
//  WindowedModifer.swift
//  DwebBrowser
//
//  Created by ui06 on 9/6/23.
//

import SwiftUI

let minWndWidth = 150.0
let minWndHeight = minWndWidth * 1.5
let maxDragWndWidth = UIScreen.main.bounds.width
let maxDragWndHeight = maxAvailableHeight

let toolAreaHeight = 30.0
let toolBtnHeight = 20.0

var maxAvailableHeight: CGFloat {
    return screen_height - safeAreaTopHeight - safeAreaBottomHeight
}

extension CGSize {
    static var wndInitSize = CGSize(width: minWndWidth, height: minWndHeight)
}

extension View {
    func windowed() -> some View {
        modifier(WindowedModifier())
    }
}

struct WindowedModifier: ViewModifier {
    @State private var wndStartSize: CGSize = .wndInitSize
    @State private var wndDragingSize: CGSize = .wndInitSize

    @State private var wndCenterOffset: CGSize = .zero

    @State private var wndStartPositionOffset: CGSize = .zero
    @State private var wndDragingPositionOffset: CGSize = .zero

    @State private var wndWidth: CGFloat = CGSize.wndInitSize.width
    @State private var wndHeight: CGFloat = CGSize.wndInitSize.height
    var isFullMode: Bool{
        wndWidth == screen_width && wndHeight == screen_height - safeAreaTopHeight - safeAreaBottomHeight
        && wndCenterOffset.width == -wndStartPositionOffset.width && wndCenterOffset.height == -wndStartPositionOffset.height
    }

    func body(content: Content) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .padding(.horizontal, 8)
                .padding(.vertical, toolAreaHeight)
                .foregroundColor(.green)

            content
                .padding(.horizontal, 8)
                .padding(.vertical, toolAreaHeight)
            if isFullMode{
                minWndButton
                    .offset(x: wndWidth/2 - toolBtnHeight + 3, y: -(wndHeight/2 - toolBtnHeight + 3))
            }else{
                maxWndButton
                    .offset(x: wndWidth/2 - toolBtnHeight + 3, y: -(wndHeight/2 - toolBtnHeight + 3))
            }
            
//            dragResizeView(isLeft: true)
            dragResizeView(isLeft: false)
        }
        .watermark(text: "desk.browser.dweb")

        .frame(width: wndWidth, height: wndHeight)
        .background(Color(white: 0.9))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .offset(wndStartPositionOffset)
        .offset(wndDragingPositionOffset)

        .offset(wndCenterOffset)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.startLocation.y - wndStartPositionOffset.height < 30 {
                        wndDragingPositionOffset.width = value.translation.width
                        wndDragingPositionOffset.height = value.translation.height
                    }
                }
                .onEnded { _ in
                    wndStartPositionOffset.width += wndDragingPositionOffset.width
                    wndStartPositionOffset.height += wndDragingPositionOffset.height
                    wndDragingPositionOffset = .zero
                }
        )
        .onChange(of: wndDragingSize) { offset in
            print("width2:\(offset.width), height:\(offset.height)")
        }
    }

    func dragResizeView(isLeft: Bool) -> some View {
        Image(systemName: isLeft ? "arrow.down.backward" : "arrow.down.right")
            .resizable()
            .frame(width: toolBtnHeight, height: toolBtnHeight)
            .foregroundColor(.black)
            .offset(x: (isLeft ? 1.0 : -1.0) * (-wndWidth/2 + toolBtnHeight - 3), y: wndHeight/2 - toolBtnHeight + 3)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        wndWidth = max(minWndWidth, min(wndStartSize.width + (value.translation.width) * (isLeft ? -1.0 : 1.0), maxDragWndWidth))
                        wndHeight = max(minWndHeight, min(wndStartSize.height + value.translation.height, maxDragWndHeight))
                        wndDragingSize = CGSize(width: wndWidth, height: wndHeight)
                    }
                    .onEnded { _ in
                        wndStartSize = wndDragingSize

                        wndWidth = wndDragingSize.width
                        wndHeight = wndDragingSize.height
                    }
            )
    }

    var maxWndButton: some View {
        Button {
            wndWidth = screen_width
            wndHeight = screen_height - safeAreaTopHeight - safeAreaBottomHeight
            wndCenterOffset.width = -wndStartPositionOffset.width
            wndCenterOffset.height = -wndStartPositionOffset.height
            wndStartSize = CGSize(width: wndWidth, height: wndHeight)
        } label: {
            Image(systemName: "arrow.up.left.and.arrow.down.right")
                .foregroundColor(.black)
        }
    }

    var minWndButton: some View {
        Button {
            wndWidth = wndDragingSize.width
            wndHeight = wndDragingSize.height

            wndCenterOffset = .zero
            print("minimize")
        } label: {
            Image(systemName: "arrow.down.forward.and.arrow.up.backward")
                .foregroundColor(.black)
        }
    }
}

struct SimpleBrowserView: View {
    @State private var isPresented = false // 初始 Z 轴索引
    var body: some View {
        ZStack {
            Color.orange
            VStack {
                Text("www.apple.com")
                Spacer()
                Button("GO") {
                    print("tap GO Button")
                }
                Button("sheet") {
                    isPresented = true
                }
            }
        }
    }
}

struct MultiResizableView: View {
    @State private var zIndexs = Array(repeating: 0, count: 3) // 初始 Z 轴索引
    @State private var wndSizes = Array(repeating: CGSize.wndInitSize, count: 3) // 初始 Z 轴索引

    @State private var maxZindex = 0 // 初始 Z 轴索引

    var body: some View {
        ZStack {
            Color.green.ignoresSafeArea()
            Color.white

            ForEach(0 ..< wndSizes.count) { i in
                SimpleBrowserView()
                    .windowed()
                    .zIndex(Double(zIndexs[i]))
                    .offset(x: CGFloat(i * 50), y: CGFloat(i * 50))
                    .onTapGesture {
                        maxZindex = zIndexs.max()!
                        zIndexs[i] = maxZindex + 1
                    }
                    .onAppear {}
            }
        }
    }
}

struct FractionFont: View {
    @State var dragAmount = 300.0
    var body: some View {
        VStack {
            Text("Hello, SwiftUI")
                .padding()
                .background(.green)
                .font(Font.system(size: 18.0))
                .scaleEffect(dragAmount/300.0) // 缩小字体大小到 90%
                .offset(y: 40)

            ZStack {
                RoundedRectangle(cornerRadius: 15)
                    .frame(width: 30, height: 30)
                    .foregroundColor(.green)
                    .offset(x: -200 + dragAmount)
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                dragAmount = value.translation.width
                            }
                    )

                Rectangle().frame(height: 3).background(.red)
            }
            .offset(y: 100)
        }
    }
}

struct ResizableView_Previews: PreviewProvider {
    static var previews: some View {
        MultiResizableView()
        FractionFont()
    }
}

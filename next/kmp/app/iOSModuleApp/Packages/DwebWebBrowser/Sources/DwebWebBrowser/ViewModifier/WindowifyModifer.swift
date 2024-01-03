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
    static var wndInit = CGSize(width: minWndWidth, height: minWndHeight)
}

extension View {
    func windowify() -> some View {
        modifier(WindowifyModifier())
    }
}

struct WindowifyModifier: ViewModifier {
    @State private var wndStartSize: CGSize = .zero
    @State private var wndDragingSize: CGSize = .zero

    @State private var wndCenterOffset: CGSize = .zero

    @State private var wndStartPositionOffset: CGSize = .zero
    @State private var wndDragingPositionOffset: CGSize = .zero

    @State private var wndWidth: CGFloat = minWndWidth
    @State private var wndHeight: CGFloat = minWndHeight
    var isFullMode: Bool{
        wndWidth == screen_width && wndHeight == screen_height - safeAreaTopHeight - safeAreaBottomHeight
        && wndCenterOffset.width == -wndStartPositionOffset.width && wndCenterOffset.height == -wndStartPositionOffset.height
    }

    func body(content: Content) -> some View {
        ZStack {
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

            dragResizeView(isLeft: false)
        }
        .watermark(text: "desk.browser.dweb")
        
        .frame(width: wndWidth, height: wndHeight)
        .background(Color(white: 0.9))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .offset(wndStartPositionOffset)
        .offset(wndDragingPositionOffset)

        .offset(wndCenterOffset)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.startLocation.y - wndStartPositionOffset.height < 30 {
                        wndDragingPositionOffset = value.translation
                    }
                }
                .onEnded { _ in
                    wndStartPositionOffset.width += wndDragingPositionOffset.width
                    wndStartPositionOffset.height += wndDragingPositionOffset.height
                    wndDragingPositionOffset = .zero
                }
        )
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
            let isFullSize = wndDragingSize.width == maxDragWndWidth &&  wndDragingSize.height == maxDragWndHeight
            if isFullSize{
                wndWidth = minWndWidth
                wndHeight = minWndHeight
            }else{
                wndWidth = wndDragingSize.width
                wndHeight = wndDragingSize.height
            }
            wndCenterOffset = .zero
            Log("minimize")
        } label: {
            Image(systemName: "arrow.down.forward.and.arrow.up.backward")
                .foregroundColor(.black)
        }
    }
}


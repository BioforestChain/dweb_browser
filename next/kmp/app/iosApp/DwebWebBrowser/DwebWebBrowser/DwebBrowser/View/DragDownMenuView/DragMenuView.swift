//
//  MenuView.swift
//  PullMenu
//
//  Created by linjie on 2024/7/29.
//

import SwiftUI

struct MenuButton: View {
    let imageName: String
    @Binding var dragSize: CGSize
    let opacity: CGFloat
    var width: Double {
        dragSize.height < minYOffsetToSelectAction ? 0 : 52
    }

    var body: some View {
        ZStack {
            MorphingShape(xDragOffset: dragSize.width)
                .frame(width: width, height: width)
                .foregroundColor(.blue)
                .opacity(opacity)
            Image(systemName: imageName)
                .fontWeight(.heavy)
                .font(.title2)
        }
        .frame(width: width, height: width)
//        .animation(.easeInOut(duration: 0.5), value: dragSize)
    }
}

struct DragMenuView: View {
    @Binding var dragSize: CGSize
    var selectedIndex: Int

    var body: some View {
        HStack {
            Spacer()
            MenuButton(imageName: "plus",
                       dragSize: $dragSize,
                       opacity: selectedIndex == 0 ? 1 : 0)
                        .foregroundStyle(.green)

            Spacer()
            MenuButton(imageName: "arrow.clockwise",
                       dragSize: $dragSize,
                       opacity: selectedIndex == 1 ? 1 : 0)
            .foregroundStyle(.green)

            Spacer()
            MenuButton(imageName: "xmark",
                       dragSize: $dragSize,
                       opacity: selectedIndex == 2 ? 1 : 0)
            .foregroundStyle(.red)

            Spacer()
        }
    }
}


#Preview {
    DragMenuView(dragSize: .constant(CGSize(width: 100, height: 100)), selectedIndex: 1)
}

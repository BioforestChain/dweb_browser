//
//  DWButton.swift
//  DwebBrowser
//
//  Created by ui06 on 5/5/23.
//

import SwiftUI

struct BiColorButton: View {
    let imageName: String
    let action: () -> Void
    @Environment(WndDragScale.self) var dragScale

    var body: some View {
        Button(action: action) {
            Image(uiImage: .assetsImage(name: imageName))
                .renderingMode(.template)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .background(.bk)
                .foregroundColor(.primary)
                .frame(width: dragScale.toolbarItemWidth, height: dragScale.toolbarItemWidth)
        }
    }
}

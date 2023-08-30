//
//  DWButton.swift
//  DwebBrowser
//
//  Created by ui06 on 5/5/23.
//

import SwiftUI

struct BiColorButton: View {
    let size: CGSize
    let imageName: String
    let disabled: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(uiImage: .assetsImage(name: (imageName)))
                    .renderingMode(.template)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .background(Color.bkColor)
                    .foregroundColor(disabled ? Color.gray : Color.ToolbarColor)
                    .frame(width: size.width, height: size.height)
            }
        }
        .disabled(disabled)
    }
}


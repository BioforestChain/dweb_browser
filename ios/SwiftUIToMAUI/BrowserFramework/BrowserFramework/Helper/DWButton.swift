//
//  DWButton.swift
//  DwebBrowser
//
//  Created by ui06 on 5/5/23.
//

import SwiftUI

struct FilledButtonStyle: ButtonStyle {
    var tintColor: Color
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.white)
            .padding()
            .background(tintColor)
            .cornerRadius(10)
            .opacity(configuration.isPressed ? 0.5 : 1.0)
    }
}

struct CloseTabStyle: ButtonStyle{
    var tintColor = Color(white: 0.95)
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.secondary)
            .opacity(configuration.isPressed ? 0.5 : 1.0)
    }
}
struct DWButton_Previews: PreviewProvider {
    static var previews: some View {
        Button("Button") {
            // Button action
        }
        .buttonStyle(FilledButtonStyle(tintColor: .red))    }
}

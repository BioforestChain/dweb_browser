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

struct BiColorButton: View {
    let size: CGSize
    let imageName: String
    let disabled: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(imageName)
                    .renderingMode(.template)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .background(Color.bkColor)
                    .foregroundColor(disabled ? Color.gray : Color.black)
                    .frame(width: size.width, height: size.height)
            }
        }
        .disabled(disabled)
    }
}

struct SimpleButton: View {
    @State private var disabled = false
    
    var body: some View {
        VStack {
            BiColorButton(size: CGSize(width: 44, height: 44), imageName: "add", disabled: disabled) {
                print("Button tapped")
            }
            
            Toggle("Disabled", isOn: $disabled)
        }
        .padding()
    }
}

struct DWButton_Previews: PreviewProvider {
    static var previews: some View {
        Button("Button") {
            // Button action
        }
        .buttonStyle(FilledButtonStyle(tintColor: .red))
        SimpleButton()        
    }
}

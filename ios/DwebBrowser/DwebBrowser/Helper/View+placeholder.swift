//
//  Placeholder_extention.swift
//  DwebBrowser
//
//  Created by ui06 on 4/26/23.
//

import SwiftUI

extension View {
    func placeholder<Content: View>(
        when shouldShow: Bool,
        alignment: Alignment = .leading,
        @ViewBuilder placeholder: () -> Content) -> some View {
            
            ZStack(alignment: alignment) {
                placeholder().opacity(shouldShow ? 1 : 0)
                self
            }
        }
}

struct Placeholder_extention: View {
    @State var inputText: String = ""

    var body: some View {
        TextField("", text: $inputText)
        
            .placeholder(when: inputText.isEmpty) {
                Text("please input").foregroundColor(Color(white: 0.8))
            }
            .padding(30)
//            .background(.orange)
    }
}

struct Placeholder_extention_Previews: PreviewProvider {
    static var previews: some View {
        Placeholder_extention()
    }
}

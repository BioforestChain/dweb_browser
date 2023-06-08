//
//  ButtonStyle_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/20.
//

import SwiftUI

extension ButtonStyle where Self == ForegroundColorStyle {
    
    static var foregroundStyle: ForegroundColorStyle {
        ForegroundColorStyle()
    }
}

struct ForegroundColorStyle: ButtonStyle {
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.foregroundColor(configuration.isPressed ? .blue : .black)
    }
}

struct ClickBackgrouoondColorStyle: ButtonStyle {
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background()
            
    }
}

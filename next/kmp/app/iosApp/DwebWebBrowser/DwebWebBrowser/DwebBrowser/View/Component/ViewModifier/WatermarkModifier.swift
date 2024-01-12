//
//  WatermarkModifier.swift
//  ResizeWindow
//
//  Created by ui06 on 8/23/23.
//

import SwiftUI

struct WatermarkModifier: View {
    var body: some View {
        Text(/*@START_MENU_TOKEN@*/"Hello, World!"/*@END_MENU_TOKEN@*/)
            .padding()
            .titleStyle()
            .watermark(text: "dweb.browser")
    }
}

extension View{
    func titleStyle() -> some View{
        modifier(Title())
    }
}


struct Title: ViewModifier{
    func body(content: Content) -> some View {
        content
            .font(.largeTitle)
            .foregroundColor(.white)
            .padding()
            .background(.blue)
            .clipShape(RoundedRectangle(cornerRadius: 18))
    }
}

extension View{
    func watermark(text: String) -> some View{
        modifier(Watermark(text: text))
    }
}

struct Watermark: ViewModifier{
    var text: String
    func body(content: Content) -> some View {
        ZStack(alignment: .bottomLeading){
            content
            Text(text)
                .font(.system(size: 8)) // 设置字体大小为24

//                .background(.white)
                .foregroundColor(.black)
                .padding(.bottom, 3)
                .padding(.leading, 10)
        }
    }
}

struct WatermarkModifier_Previews: PreviewProvider {
    static var previews: some View {
        WatermarkModifier()
    }
}

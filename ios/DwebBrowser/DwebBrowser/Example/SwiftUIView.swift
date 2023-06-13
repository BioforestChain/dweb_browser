//
//  SwiftUIView.swift
//  DwebBrowser
//
//  Created by ui06 on 6/9/23.
//

import SwiftUI

struct SwiftUIView: View {
    var image: Image
    @State var isBig = false
    var body: some View {
        ZStack{
            ZStack{
                ForEach(0..<10) { index in
                    
                    Rectangle()
                        .overlay(
                        Image("xxxx")
                            .resizable()
                            .frame(width: 230, height: 166)
                            .scaleEffect(x: isBig ? 5 : 1, y: isBig ? 5 : 1)
                    )
                    .frame(width: isBig ? 166*5 : 230, height: isBig ? 166*5 : 166)
                    .animation(.linear, value: isBig)
                    .background(.green)
                    .offset(x: CGFloat(index*5),y: CGFloat(index*5))
                }
            }
            VStack{
                Spacer()
                Button {
                    withAnimation {
                        isBig.toggle()
                    }
                } label: {
                    Text("Tap Me")
                }
            }
        }
    }
}

struct SwiftUIView_Previews: PreviewProvider {
    static var previews: some View {
        SwiftUIView(image: Image("xxxx"))
    }
}

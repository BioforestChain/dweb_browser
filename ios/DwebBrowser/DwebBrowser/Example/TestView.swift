//
//  TestView.swift
//  DwebBrowser
//
//  Created by ui06 on 5/9/23.
//

import SwiftUI

import SwiftUI
var colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple]
let width: CGFloat = UIScreen.main.bounds.width

struct TestView: View {
    @State var offset: CGFloat
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(0..<colors.count) { index in
                    Rectangle()
                        .fill(colors[index])
                        .frame(width: width)
                        .gesture(
                            DragGesture()
                                .onEnded { value in
                                    if value.translation.width < 0 {
                                        withAnimation {
                                            offset += width
                                        }
                                    }
                                    if value.translation.width > 0 {
                                        withAnimation {
                                            offset += width
                                        }
                                    }
                                }
                        )
                }
            }
            .frame(width: width)
            .animation(.spring())
            
            .onAppear {
                UIScrollView.appearance().isPagingEnabled = true
            }
            .offset(CGSize(width:offset, height:0))
        }
    }
    
    func scrollForward() {
        let lastColor = colors.last!
        colors.removeLast()
        colors.insert(lastColor, at: 0)
    }
    
    func scrollBackward() {
        let firstColor = colors.first!
        colors.removeFirst()
        colors.append(firstColor)
    }
}

//struct ContentView_Previews: PreviewProvider {
//    static var previews: some View {
//        ContentView()
//    }
//}

struct TestView_Previews: PreviewProvider {
    static var previews: some View {
        TestView(offset: width/2.0)
    }
}

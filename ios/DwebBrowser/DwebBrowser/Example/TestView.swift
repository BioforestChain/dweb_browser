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

struct ZoomAnimationView: View{
    @State var isZoomed = false
    @Namespace private var animation
    var frame: CGFloat{
        isZoomed ? 300 : 44
    }
    
    var body: some View{
        VStack{
            Spacer()
            VStack{
                HStack{
                    RoundedRectangle(cornerRadius: 10)
                        .fill(.red)
                        .frame(width: frame, height: frame)
                        .padding(.top, isZoomed ? 20 : 0)
                    
                    if !isZoomed {
                        Text("Jay Chow")
                            .font(.headline)
                            .matchedGeometryEffect(id: "name", in: animation)
                        Spacer()
                    }
                }
                
                if isZoomed{
                    Text("Jay Chow")
                        .font(.headline)
                        .matchedGeometryEffect(id: "name", in: animation)
                        .padding(.bottom,60)
                    
                    Spacer()
                }
            }
            .onTapGesture {
                withAnimation(.spring(),{
                    isZoomed.toggle()
                })
            }
            .padding()
            .frame(maxWidth: .infinity)
            .frame(height: isZoomed ? 400 : 60)
            .background(Color(white: 0.8))
        }
    }
}

struct TestView_Previews: PreviewProvider {
    static var previews: some View {
                TestView(offset: width/2.0)
//        TakeSnapShotView()
    }
}

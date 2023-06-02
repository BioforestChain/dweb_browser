//
//  AutoFillView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/28/23.
//

import SwiftUI

//struct AutoFillView: View {
//    var body: some View {
//        Text(/*@START_MENU_TOKEN@*/"Hello, World!"/*@END_MENU_TOKEN@*/)
//    }
//}

struct AutoFillView: View {
    @State private var v2Height: CGFloat = 50
    
    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 0) {
//                VStack {
                    Text("View 1")
                        .frame(maxWidth: .infinity, maxHeight: v2Height == 0 ? .infinity : geo.size.height * 0.5)
                        .background(Color.green)
//                    Spacer()
//                }
//                .onAppear {
//                    if v2Height == 0 {
//                        v2Height = geo.size.height * 0.5
//                    }
//                }
                
                Text("View 2")
                    .frame(maxWidth: .infinity, maxHeight: v2Height)
                    .background(Color.yellow)
//                    .onDisappear {
//                        if v2Height == geo.size.height * 0.5 {
//                            v2Height = 0
//                        }
//                    }
                
                Text("View 3")
                    .frame(maxWidth: .infinity, maxHeight: 50)
                    .background(Color.blue)
                    .onTapGesture {
                        v2Height = 0
                    }
            }
        }
    }
}

struct AutoFillView_Previews: PreviewProvider {
    static var previews: some View {
        AutoFillView()
    }
}

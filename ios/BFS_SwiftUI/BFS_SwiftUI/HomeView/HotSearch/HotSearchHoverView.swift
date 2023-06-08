//
//  HotSearchHoverView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

struct HotSearchHoverView: View {
    
    @State private var timer = Timer.publish(every: 1.5, on: .main, in: RunLoop.Mode.common).autoconnect()
    @State private var bgColor = SwiftUI.Color(white: 0.93, opacity: 1.0)
    @State var isInvalide: Bool = false
    var body: some View {
        
        HStack {
            Rectangle()
                .fill(bgColor)
                .onReceive(timer) { output in
                    if isInvalide {
                        timer.upstream.connect().cancel()
                    }
                    withAnimation(.easeInOut(duration: 1.0)) {
                        bgColor = SwiftUI.Color(white: 0.9, opacity: 1.0)
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        withAnimation(.easeInOut(duration: 0.5)) {
                            bgColor = SwiftUI.Color(white: 0.93, opacity: 1.0)
                        }
                    }
                }
        }
    }
}

struct HotSearchHoverView_Previews: PreviewProvider {
    static var previews: some View {
        HotSearchHoverView()
    }
}

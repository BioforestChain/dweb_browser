//
//  ScrollPageContainer.swift
//  DwebBrowser
//
//  Created by ui06 on 5/18/23.
//

import SwiftUI

struct PagingScroll<Content: View>: UIViewRepresentable {
    var contentSize: Int
    @Binding var offsetX: CGFloat
    @Binding var indexInEvrm: Int
    var content: ()-> Content

    func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView()
        scrollView.isPagingEnabled = true
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.delegate = context.coordinator
        
        return scrollView
    }
    
    func updateUIView(_ uiView: UIScrollView, context: Context) {
        let actualOffsetX = $offsetX.wrappedValue
        if abs(actualOffsetX / screen_width).truncatingRemainder(dividingBy: 1) == 0
            {
            
            uiView.subviews.forEach { $0.removeFromSuperview() }
            let hostingController = UIHostingController(rootView: content())
            for i in 0..<contentSize {
                let childView = hostingController.view!
                // There must be an adjustment to fix an unknown bug that causes a strange offset problem.
                // probably is because the alignment of swiftui is central
                let adjustment = CGFloat((contentSize - 1)) * screen_width/2.0
                let xOffset = screen_width * CGFloat(i) - adjustment
                childView.frame = CGRect(x: xOffset, y: 0, width: screen_width, height: 50)
                uiView.addSubview(childView)
            }

            uiView.contentSize = CGSize(width: screen_width * CGFloat(contentSize), height: 0)
            uiView.setContentOffset(CGPoint(x: CGFloat(indexInEvrm) * screen_width, y: 0), animated: true)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIScrollViewDelegate {
        var parent: PagingScroll
        init(_ parent: PagingScroll) {
            self.parent = parent
        }
        
        func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
            let page = Int(scrollView.contentOffset.x / scrollView.frame.width)
            //FIXME: Publishing changes from within view updates is not allowed, this will cause undefined behavior.
            DispatchQueue.main.async { [self] in
                self.parent.indexInEvrm = page
            }
        }
        
        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            //FIXME: Publishing changes from within view updates is not allowed, this will cause undefined behavior.
            DispatchQueue.main.async {  [self] in
                self.parent.offsetX = -( scrollView.contentOffset.x)
            }
        }
    }
}

//let screen_width = UIScreen.main.bounds.width

//struct PagingScrollTestView: View {
//    @State var currentPage = 0
//    @State var offsetX: CGFloat = 0.0
//
//    @State private var randomIndex = 0
//    var body: some View {
//        VStack {
//            PagingScroll(contentSize: 5, content: HStack(spacing: 0) {
//                Color.red.frame(width: screen_width, height: 50)
//                Color.green.frame(width: screen_width, height: 50)
//                Color.blue.frame(width: screen_width, height: 50)
//                Color.orange.frame(width: screen_width, height: 50)
//                Color.cyan.frame(width: screen_width, height: 50)
//            }, currentPage: $currentPage,offsetX: $offsetX)
//
//            Button("Random") {
//
//                    currentPage = Int.random(in: Range(0...4))
//            }
//
//            HStack {
//                Button("Prev") {
//                    if currentPage > 0 {
//                        currentPage -= 1
//                    }
//                }
//                Spacer()
//                Text("Page \(currentPage + 1)")
//                Spacer()
//                Button("Next") {
//                    if currentPage < 2 {
//                        currentPage += 1
//                    }
//                }
//            }.padding()
//        }
//    }
//}

struct ContentView_Previews9: PreviewProvider {
    static var previews: some View {
//        PagingScrollTestView()
        Text("")
    }
}

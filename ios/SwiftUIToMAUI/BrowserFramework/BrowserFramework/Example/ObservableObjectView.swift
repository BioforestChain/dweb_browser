//
//  ContentView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//
import Combine
import SwiftUI

class MyModel: ObservableObject {
    var didChange = PassthroughSubject<Void, Never>()

    var count = 0 {
        didSet {
            didChange.send()
        }
    }

    func increment() {
        count += 1
    }
}

struct MyView: View {
    @ObservedObject var model: MyModel

    var body: some View {
        VStack {
            Text("Count: \(model.count)")
            Button(action: {
                self.model.increment()
            }) {
                Text("Increment")
            }
        }
    }
}

struct ContentView: View {
    @State var model = MyModel()

    var body: some View {
        MyView(model: model)
    }
}


struct HContainer:View{
    var body: some View{
        HStack(spacing: 0) {
            Color.red
                .frame(width: screen_width)
            Color.green
                .frame(width: screen_width)
            Color.blue
            .frame(width: screen_width)
        }
        .background(.orange)
    }
}


struct PageScroll<Content: View>: UIViewRepresentable {
    
    var contentSize: Int
    var content: Content
    
    func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView()
        scrollView.isPagingEnabled = true
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.contentSize = CGSize(width: screen_width * CGFloat(contentSize), height: 0)
        
        return scrollView
    }
    
    func updateUIView(_ uiView: UIScrollView, context: Context) {
        
        uiView.subviews.forEach { $0.removeFromSuperview() }
        let hostingController = UIHostingController(rootView: content)
        for i in 0..<contentSize {
            let childView = hostingController.view!
            // There must be an adjustment to fix an unknown reason that is causing a strange bug.
            let adjustment = CGFloat((contentSize - 1)) * screen_width/2.0
            childView.frame = CGRect(x: screen_width * CGFloat(i) - adjustment, y: 0, width: screen_width, height: 50)
            uiView.addSubview(childView)
        }
    }
}


struct PagingScrollContent: View {
    
    var body: some View {
        PageScroll(contentSize: 3, content: HContainer())
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        PagingScrollContent()
    }
}

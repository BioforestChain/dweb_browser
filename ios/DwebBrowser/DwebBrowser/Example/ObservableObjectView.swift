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

//
//  ContentView.swift
//  SwiftUITest
//
//  Created by instinct on 2024/4/1.
//

import Observation
import SwiftUI
import WebKit

struct WebContainerView: View {
    let viewModel = ContentViewModel()
    @State private var offSet: CGFloat = .zero
    // scrollIndex: 是一个Int类型的hash值，用来控制滚动到那个page上的。
    @State private var scrollIndex: Int? = 0
    @State private var show = false

    var body: some View {
        ContainerView(index: $scrollIndex) {
            Button(action: {
                scrollIndex = viewModel.scrollIndex(by: 0)
//                withAnimation {
//                    scrollIndex = viewModel.scrollIndex(by: 0)
//                }
            }, label: {
                Image(systemName: "plus")
                    .tint(.black)
                    .font(.title2)
            })

        } rightBuilder: {
            HStack {
                Button(action: {
                    print("more action")
                    withAnimation {
                        scrollIndex = viewModel.scrollIndex(by: 2)
                    }
                }, label: {
                    ZStack {
                        Image(systemName: "square.on.square")
                            .font(.title2)
                            .rotationEffect(Angle(degrees: 270))
                            .scaleEffect(CGSize(width: 1.2, height: 1.2))
                        Text(viewModel.pageCountString)
                            .font(viewModel.pageCountFont)
                            .offset(x: 2.5, y: -3)
                    }
                    .tint(.black)
                })

                Button(action: {
                    show.toggle()
                }, label: {
                    Image(systemName: "ellipsis")
                        .tint(.black)
                        .font(.title2)
                        .rotationEffect(Angle(degrees: 90))
                        .popover(isPresented: $show,
                                 attachmentAnchor: .point(UnitPoint(x: 0, y: -2)),
                                 content: {
                                     ContainerMoreMenu(datas: viewModel.moreItems,
                                                       traceless: viewModel.tracelessBinding,
                                                       show: $show)
                                         .presentationCompactAdaptation(.popover)
                                         .frame(minWidth: 200)
                                         .padding()
                                 })
                })
            }

        } pageViewBuilder: {
            LazyHStack(spacing: 0.0, content: {
                ForEach(viewModel.pages, id: \.id) { page in
                    PageView(url: page.url)
                }
            })
        } addressViewBuilder: {
            LazyHStack(spacing: 0.0, content: {
                ForEach(viewModel.pages, id: \.id) { page in
                    let address = page.address
                    AddressView(model: address, inputText: address.textBinding)
                }
            })
        }
        .onChange(of: scrollIndex) { _, newValue in
//            print("receive: index = \(newValue)")
        }
    }
}



//struct ColorListView: View {
//    let bgColors: [Color] = [.yellow, .blue, .orange, .indigo, .green]
//    @State private var scrollID: Int?
//    var body: some View {
//        VStack(content: {
//            Text("\(scrollID ?? -1)")
//            ScrollView {
//                LazyVStack(spacing: 10) {
//                    ForEach(0 ... 50, id: \.self) { index in
//
//                        bgColors[index % 5]
//                            .frame(height: 100)
//                            .overlay {
//                                Text("\(index)")
//                                    .foregroundStyle(.white)
//                                    .font(.system(.title, weight: .bold))
//                            }
//                            .onTapGesture {
//                                withAnimation {
//                                    scrollID = 0
//                                }
//                            }
//                    }
//                }
//                .scrollTargetLayout()
//            }
//            .contentMargins(.horizontal, 10.0, for: .scrollContent)
//            .scrollPosition(id: $scrollID)
//            .onChange(of: scrollID) { _, newValue in
//                print(newValue ?? "")
//            }
//        })
//    }
//}
//
//#Preview(body: {
//    ColorListView()
//})
//
//struct GalleryScrollTargetBehavior: ScrollTargetBehavior {
//    func updateTarget(_ target: inout ScrollTarget, context: TargetContext) {
//        if target.rect.minY < (context.containerSize.height / 3.0),
//           context.velocity.dy < 0.0 {
//            target.rect.origin.y = 0.0
//        }
//    }
//}
//
//
//
//





//#Preview {
//    ContentView()
//}
//
//struct DContentView: View {
//    @State private var offset = CGFloat.zero
//
//    var body: some View {
//        HStack(alignment: .top) {
//            // MainScrollView
//            ScrollView {
//                VStack {
//                    ForEach(0..<100) { i in
//                        Text("Item \(i)").padding()
//                    }
//                }
//                .background(GeometryReader {
//                    Color.clear.preference(key: ViewOffsetKey.self,
//                                           value: -$0.frame(in: .named("scroll")).origin.y)
//                })
//                .onPreferenceChange(ViewOffsetKey.self) { value in
//                    print("offset >> \(value)")
//                    offset = value
//                }
//            }
//            .coordinateSpace(name: "scroll")
//
//            // Synchronised with ScrollView above
//            ScrollView {
//                VStack {
//                    ForEach(0..<100) { i in
//                        Text("Item \(i)").padding()
//                    }
//                }
//                .offset(y: -offset)
//            }
//            .disabled(true)
//        }
//    }
//}

//protocol BindinggableDataType {}
//extension Bool: BindinggableDataType {}
//extension Int: BindinggableDataType {}
//extension Int8: BindinggableDataType {}
//extension Int16: BindinggableDataType {}
//extension Int32: BindinggableDataType {}
//extension Int64: BindinggableDataType {}
//extension UInt: BindinggableDataType {}
//extension UInt8: BindinggableDataType {}
//extension UInt16: BindinggableDataType {}
//extension UInt32: BindinggableDataType {}
//extension UInt64: BindinggableDataType {}
//extension Float: BindinggableDataType {}
//extension Double: BindinggableDataType {}
//extension String: BindinggableDataType {}
//
//protocol BindinggableProtocol: AnyObject {
//    associatedtype Value: BindinggableDataType
//    func toBinding(keyPath: WritableKeyPath<Self, Value>) -> Binding<Value>
//}
//
//extension BindinggableProtocol {
//    func toBinding(keyPath: WritableKeyPath<Self, Value>) -> Binding<Value> {
//        Binding { [unowned self] in
//            self[keyPath: keyPath]
//        } set: { [weak self] newValue in
//            self?[keyPath: keyPath] = newValue
//        }
//    }
//}

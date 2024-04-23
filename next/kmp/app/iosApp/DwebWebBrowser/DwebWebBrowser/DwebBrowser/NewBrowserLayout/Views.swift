//
//  Views.swift
//  SwiftUITest
//
//  Created by instinct on 2024/4/7.
//

import Foundation
import SwiftUI
import WebKit

struct ViewOffsetKey: PreferenceKey {
    typealias Value = CGFloat
    static var defaultValue = CGFloat.zero
    static func reduce(value: inout Value, nextValue: () -> Value) {
        value += nextValue()
    }
}

struct ContainerView<Left: View, Right: View, PageContainer: View, AddressContainer: View>: View {
    @State private var offSet: CGFloat = .zero
    @Binding var selectedIndex: Int?

    let pageViewBuilder: () -> PageContainer
    let addressViewBuilder: () -> AddressContainer
    let leftBuilder: () -> Left
    let rightBuilder: () -> Right

    init(index: Binding<Int?>,
         @ViewBuilder leftBuilder: @escaping () -> Left,
         @ViewBuilder rightBuilder: @escaping () -> Right,
         @ViewBuilder pageViewBuilder: @escaping () -> PageContainer,
         @ViewBuilder addressViewBuilder: @escaping () -> AddressContainer) {
        self._selectedIndex = index
        self.pageViewBuilder = pageViewBuilder
        self.addressViewBuilder = addressViewBuilder
        self.leftBuilder = leftBuilder
        self.rightBuilder = rightBuilder
    }

    var body: some View {
        VStack {
            Text("\(selectedIndex ?? -1)")

            ScrollView(.horizontal) {
                pageViewBuilder()
                    .offset(x: offSet)
            }
            .scrollDisabled(true)

            HStack(spacing: 0.0) {
                leftBuilder()
                    .frame(maxHeight: .infinity)
                    .containerRelativeFrame(.horizontal) { l, axis in
                        axis == .horizontal ? l * 0.1 : l
                    }

                ScrollView(.horizontal) {
                    addressViewBuilder()
                        .scrollTargetLayout()
                        .background(GeometryReader {
                            Color.clear.preference(key: ViewOffsetKey.self,
                                                   value: $0.frame(in: .named("scroll")).origin.x)
                        })
                        .onPreferenceChange(ViewOffsetKey.self) { value in
                            offSet = value * 10 / 7
                        }
                }
                .scrollPosition(id: $selectedIndex)
                .coordinateSpace(name: "scroll")
                .containerRelativeFrame(.horizontal) { l, axis in
                    axis == .horizontal ? l * 0.7 : l
                }
                .scrollTargetBehavior(.paging)
                .scrollIndicators(.never)

                rightBuilder()
                    .frame(maxHeight: .infinity)
                    .containerRelativeFrame(.horizontal) { l, axis in
                        axis == .horizontal ? l * 0.2 : l
                    }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
        }
    }
}

struct PageWebView: UIViewRepresentable {
    
    var urlString: String?
    
    class Coordinator: NSObject, WKNavigationDelegate {
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            print("web loaded....")
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let web =  WKWebView(frame: CGRect.zero, configuration: config)
        web.navigationDelegate = context.coordinator
        return web
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        guard let urlString = urlString, let url = URL(string: urlString) else { return }
        let request = URLRequest(url: url)
        uiView.load(request)
    }
}

struct PageView: View {
    @State private var url: DwebUrl
    var body: some View {
        Group(content: {
            switch url {
                case .blank:
                    Text("blank")
                case .privacy:
                    Text("privacy")
                case .history:
                    Text("history")
                case .url(let urlStr, _):
                    PageWebView(urlString: urlStr)
            }
        })
        .containerRelativeFrame([.horizontal, .vertical])
        .fullSpace()
        .border(.red)
    }
    init(url: DwebUrl) {
        self.url = url
    }
}

#Preview(body: {
    BookmarkView()
})


extension View {
    func fullSpace() -> some View {
        modifier(DwebFullSpace())
    }
}

struct DwebFullSpace: ViewModifier {
    func body(content: Content) -> some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

enum DwebImage {
    case systerm(String)
    case asset(String)
    
    var image: Image {
        switch self {
            case .systerm(let string):
                Image(systemName: string)
            case .asset(let string):
                Image(string)
        }
    }
}

struct AddressView: View {
    let model: AddressModel
    @Binding var inputText: String
    var body: some View {
        ZStack {
            Spacer()
                .containerRelativeFrame([.horizontal, .vertical])
            HStack {
                Spacer()
                model.leftImage.image
                Spacer()
                TextField("", text: model.textBinding, prompt: Text("请输入网址或者搜索"))
                    .fullSpace()
                Spacer()
                Button(action: {
                    model.rightButtonAction(model.textBinding.wrappedValue)
                }, label: {
                    model.rightImage.image
                })
                .tint(.black)
                Spacer()
            }.background(content: {
                Color.white
            })
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .containerRelativeFrame([.horizontal, .vertical], alignment: .center) { l, axis in
                switch axis {
                case .horizontal:
                    l * 0.95
                case .vertical:
                    l * 0.8
                }
            }
            .cornerRadius(5)
            .background {
                Color.white
                    .cornerRadius(5)
                    .shadow(color: .black.opacity(0.3), radius: 3, y:3)
            }
        }
    }
}

struct ContainerMoreMenu: View {
    let datas: [ContainerMoreItem]
    @Binding var traceless: Bool
    @Binding var show: Bool
    var body: some View {
        VStack(alignment: .leading, content: {
            ForEach(datas, id: \.title) { data in
                ContainerMoreMenuItemView(data: data, traceless: $traceless)
                    .onTapGesture {
                        show = false
                        data.doAction()
                    }
            }
        })
    }
}

struct ContainerMoreMenuItemView: View {
    let data: ContainerMoreItem
    @Binding var traceless: Bool
    var body: some View {
        HStack {
            Image(systemName: data.icon)
            Text(data.title)
            Spacer()
            switch data {
            case .bookmark, .privacy(_), .download, .history:
                Image(systemName: "chevron.right")
            case .tackless(_, _):
                Toggle("", isOn: $traceless)
            default:
                let _ = print("")
            }
        }
        .tint(.black)
        .frame(height: 50)
    }
}

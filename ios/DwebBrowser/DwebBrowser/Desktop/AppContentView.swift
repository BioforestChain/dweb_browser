//
//  AppContentView.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import SwiftUI

struct AppContentView: View {
    
    @Namespace var animation
    var idString: String
    @State var isExpand: Bool = false
    @EnvironmentObject var configViewModel: ConfigViewModel
    @EnvironmentObject var openViewModel: OpenAppViewModel
    
    var scale: CGFloat {
        UIScreen.main.bounds.width / UIScreen.main.bounds.height
    }
    
    var body: some View {
        
        GeometryReader { proxy in
            VStack(spacing: 0) {
                
                if isExpand {
                    TabView()
                        .background(.secondary)
                        .frame(maxWidth: .infinity)
                        .cornerRadius(8)
                        .frame(height: 42)
                        .padding(.top, 10)
                        .matchedGeometryEffect(id: "TAB", in: animation)
                } else {
                    HStack {
                        TabView()
                            .background(.secondary)
                            .frame(width: 200,height: 22)
                            .cornerRadius(4)
                            .padding(.top, 50)
                            .padding(.leading, 20)
                            .matchedGeometryEffect(id: "TAB", in: animation)
                        Spacer()
                    }
                }
                
                BrowserView()
                    .transformEffect(isExpand ? .identity : CGAffineTransform(scaleX: 0.8, y: 0.8))
                    .padding(.top, 10)
                    .padding(.leading, isExpand ? 0 : 20)
                    .padding(.trailing,isExpand ? 0 : 60)
                    .padding(.bottom,isExpand ? 0 : 100)
            }
            .clipped()
            .edgesIgnoringSafeArea(.bottom)
        }
    }
    
    @ViewBuilder
    private func TabView() -> some View {
        let images: [String] = ["trash","clock.arrow.circlepath","bell.badge"]
        GeometryReader { proxy in
            HStack(spacing: 0) {
                ForEach(images, id: \.self) { name in
                    Button {
                        //删除
                        if name == "trash" {
                            if let index = openViewModel.apps.firstIndex(where: {$0.id == idString }) {
                                _ = openViewModel.apps.remove(at: index)
                            }
                            if openViewModel.apps.count == 0 {
                                configViewModel.selectedTab = ""
                            } else {
                                if let last = openViewModel.apps.last {
                                    configViewModel.selectedTab = last.id
                                }
                            }
                            withAnimation {
                                configViewModel.showMenu = false
                            }
                        }
                        //app图标，点击显示app菜单栏
                        if name == "clock.arrow.circlepath" {
                            withAnimation {
                                configViewModel.showMenu.toggle()
                            }
                        }
                        //app界面放大、缩小
                        if name == "bell.badge" {
                            withAnimation {
                                configViewModel.showMenu = false
                                isExpand.toggle()
                            }
                        }
                    } label: {
                        Image(systemName: name)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: isExpand ? 22 : 16)
                            .foregroundColor(.white)
                            .padding(.horizontal, (proxy.size.width / 3 - (isExpand ? 22 : 16)) * 0.5)
                            .padding(.vertical, isExpand ? 8 : 4)
                            .matchedGeometryEffect(id: name, in: animation)
                            
                    }

                }
            }
        }
    }
}

struct AppContentView_Previews: PreviewProvider {
    static var previews: some View {
        AppContentView(idString: "")
    }
}


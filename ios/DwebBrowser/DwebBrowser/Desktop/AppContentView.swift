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
    @State var isContract: Bool = false
    @EnvironmentObject var configViewModel: ConfigViewModel
    @EnvironmentObject var openViewModel: OpenAppViewModel
    
    var scale: CGFloat {
        UIScreen.main.bounds.width / UIScreen.main.bounds.height
    }
    
    var body: some View {
        
        GeometryReader { proxy in
            VStack(alignment: .leading, spacing: 0) {
                
                if isExpand {
                    if configViewModel.isContract {
                        HStack {
                            Spacer()
                            TabView(isContract: true)
                                .background(.primary)
                                .frame(width: 80, height: 32)
                                .cornerRadius(16)
                                .padding(.top, 10)
                                .padding(.trailing, 20)
                                .matchedGeometryEffect(id: "TAB", in: animation, anchor: .leading)
                        }
                    } else {
                        TabView(isContract: false)
                            .background(.primary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 42)
                            .padding(.top, 10)
                            .matchedGeometryEffect(id: "TAB", in: animation, anchor: .leading)
                    }
                } else {
                    HStack {
                        TabView(isContract: false)
                            .background(.primary)
                            .frame(width: proxy.size.width * 0.8,height: 22)
                            .padding(.top, 50)
                            .matchedGeometryEffect(id: "TAB", in: animation, anchor: .leading)
                        Spacer()
                    }
                }
                
                BrowserView()
                    .scaleEffect(isExpand ? 1.0 : 0.8, anchor: .topLeading)
                    .transition(.scale)
                    .overlay(overLayView)
            }
            .clipped()
            
        }
    }
    
    @ViewBuilder
    private var overLayView: some View {
        if !configViewModel.isContract {
            RoundedRectangle(cornerRadius: 0)
                .fill(Color.black.opacity(0.1))
                .scaleEffect(isExpand ? 1.0 : 0.8, anchor: .topLeading)
                .transition(.scale)
                .onTapGesture {
                    withAnimation {
                        configViewModel.isContract.toggle()
                    }
                }
        }
    }
    
    @ViewBuilder
    private func TabView(isContract: Bool) -> some View {
        let images: [String] = iconImages(isContract: isContract)
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
                        //打开折叠
                        if name == "chevron.left.circle" {
                            withAnimation {
                                configViewModel.isContract = false
                            }
                        }
                    } label: {
                        Image(systemName: name)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: isExpand ? 22 : 16)
                            .foregroundColor(.white)
                            .padding(.horizontal, (proxy.size.width / CGFloat(images.count) - (isExpand ? 22 : 16)) * 0.5)
                            .padding(.vertical, isExpand ? 8 : 4)
                            .matchedGeometryEffect(id: name, in: animation)
                            
                    }

                }
            }
        }
    }
    
    private func iconImages(isContract: Bool) -> [String] {
        var images: [String] = ["trash","clock.arrow.circlepath","bell.badge"]
        if isContract {
            images = ["trash","chevron.left.circle"]
        }
        return images
    }
}

struct AppContentView_Previews: PreviewProvider {
    static var previews: some View {
        AppContentView(idString: "")
    }
}


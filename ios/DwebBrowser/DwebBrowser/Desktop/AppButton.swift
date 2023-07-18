//
//  AppButton.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import SwiftUI

struct AppButton: View {
    
    var image: String
    var idString: String
    @Namespace var animation
    @EnvironmentObject var configViewModel: ConfigViewModel
    @EnvironmentObject var openViewModel: OpenAppViewModel
    
    var body: some View {
        
        Button {
            withAnimation {
                configViewModel.showMenu = false
            }
            withAnimation(.spring().delay(0.3)) {
                configViewModel.selectedTab = idString
                
            }
            if let index = openViewModel.apps.firstIndex(where: {$0.id == idString }) {
                let obj = openViewModel.apps.remove(at: index)
                openViewModel.apps.append(obj)
            } else {
                let obj = AppContentModel(id: idString)
                openViewModel.apps.append(obj)
            }
        } label: {
            Image(systemName: image)
                .font(.title)
                .frame(width: 50)
                .foregroundColor(configViewModel.selectedTab == idString ? .white : .blue)
            .padding(.vertical, 15)
            .padding(.horizontal, 15)
            .frame(width: 70)
            .background(
                ZStack {
                    if configViewModel.selectedTab == idString {
                        Color.blue
                            .opacity(configViewModel.selectedTab == idString ? 1 : 0)
                            .clipShape(
                                CustomCorners(corners: [.topLeft, .bottomLeft], radius: 10)
                            )
                            .matchedGeometryEffect(id: "TAB", in: animation)
                    }
                }
            )
        }

    }
}



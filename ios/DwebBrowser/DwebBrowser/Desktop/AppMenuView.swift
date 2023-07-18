//
//  AppMenuView.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/14.
//

import SwiftUI

struct AppMenuView: View {
    
    var body: some View {
        VStack(alignment: .center,spacing: 10) {
            ForEach(apps, id: \.id) { app in
                AppButton(image: app.imageName, idString: app.id)
            }
        }
        .background(
            ZStack {
                Color.white
                    .clipShape(
                        CustomCorners(corners: [.topLeft, .bottomLeft], radius: 10)
                    )
            }
        )
    }
    
    let apps: [AppDataModel] = [
        AppDataModel(name: "House", imageName: "house", id: "1"),
        AppDataModel(name: "History", imageName: "clock.arrow.circlepath", id: "2"),
        AppDataModel(name: "Notifications", imageName: "bell.badge", id: "3"),
        AppDataModel(name: "Settings", imageName: "gearshape.fill", id: "4"),
        AppDataModel(name: "Help", imageName: "questionmark.circle", id: "5")
    ]
}


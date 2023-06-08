//
//  SheetView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftUI

struct SheetView: View {
    
    private let images = ["ico_bottomtab_book_normal","ico_menu_history_normal","ico_menu_share_normal"]
    private let titles = ["书签","历史记录","分享"]
    @State private var isPresented = false
    @State private var sheetAction: SheetAction?
    
    var urlString: String
    
    var body: some View {
        
        VStack {
            Spacer()
            HStack(alignment: .bottom) {
                Spacer()
                ForEach(0...2, id: \.self) { index in
                    
                    Spacer()
                    Button {
                        handleButtonAction(index: index)
                        self.isPresented.toggle()
                    } label: {
                        VStack(alignment: .center, spacing: 8) {
                            
                            ZStack {
                                SwiftUI.Color.white
                                    .frame(width: UIScreen.main.bounds.width / 7, height: UIScreen.main.bounds.width / 7)
                                    .cornerRadius(10)
                                    .padding(EdgeInsets(top: 50, leading: 0, bottom: 0, trailing: 0))
                                
                                Image(images[index])
                                    .resizable()
                                    .frame(width: 30, height: 30)
                                    .padding(EdgeInsets(top: 50, leading: 0, bottom: 0, trailing: 0))
                            }
                            
                            
                            Text(titles[index])
                                .font(.system(size: 13.0))
                                .foregroundColor(.black)
                                .padding(EdgeInsets(top: 0, leading: 0, bottom: 50, trailing: 0))
                        }
                    }
                    .sheet(item: $sheetAction) { action in
                        getActionView(action)
                    }
                    Spacer()
                }
                Spacer()
            }
            .background(Color(hexString:"F5F6F7"))
        }
        .edgesIgnoringSafeArea(.bottom)
        
    }
    
    private func handleButtonAction(index: Int) {
        if index == 2 {
            guard let url = URL(string: urlString) else { return }
            sharePage(shareLink: url)
        } else {
            sheetAction = .view(text: titles[index])
        }
    }
    
    // 获取显示的视图
    private func getActionView(_ action: SheetAction) -> some View {
        
        switch action {
        case .view(let text):
            
            var type: NoResultEnum = .none
            var viewModel = HistoryViewModel()
            if text == "书签" {
                viewModel = BookmarkViewModel()
                type = .bookmark
            } else if text == "历史记录" {
                viewModel = LinkHistoryViewModel()
                type = .linkHistory
            }
            return HistoryListView(viewModel: viewModel, type: type)
        }
    }
    // 分享视图
    private func sharePage(shareLink: URL) {
        
        let activityViewController : UIActivityViewController = UIActivityViewController(
            activityItems: [ shareLink ], applicationActivities: nil)
        
        // This line remove the arrow of the popover to show in iPad
        activityViewController.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection.down
        activityViewController.popoverPresentationController?.sourceRect = CGRect(x: 150, y: 150, width: 0, height: 0)
        
        // Pre-configuring activity items
        if #available(iOS 13.0, *) {
            activityViewController.activityItemsConfiguration = [
                UIActivity.ActivityType.message
            ] as? UIActivityItemsConfigurationReading
        } else {
            // Fallback on earlier versions
        }
        
        // Anything you want to exclude
        activityViewController.excludedActivityTypes = [
            UIActivity.ActivityType.postToWeibo,
            UIActivity.ActivityType.print,
            UIActivity.ActivityType.assignToContact,
            UIActivity.ActivityType.saveToCameraRoll,
            UIActivity.ActivityType.addToReadingList,
            UIActivity.ActivityType.postToFlickr,
            UIActivity.ActivityType.postToVimeo,
            UIActivity.ActivityType.postToTencentWeibo,
            UIActivity.ActivityType.postToFacebook
        ]
        
        if #available(iOS 13.0, *) {
            activityViewController.isModalInPresentation = true
        } else {
            // Fallback on earlier versions
        }
        
        UIApplication.shared.keyWindow?.rootViewController?.present(activityViewController, animated: true)
    }
}

enum SheetAction: Identifiable {
    
    case view(text: String)
    
    var id: UUID {
        UUID()
    }
}

struct SheetView_Previews: PreviewProvider {
    static var previews: some View {
        SheetView(urlString: "http://www.baidu.com")
    }
}



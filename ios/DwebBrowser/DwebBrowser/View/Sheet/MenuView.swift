//
//  MenuView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI

struct MenuView: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @State private var isTraceless: Bool = TraceLessMode.shared.isON
    @State var isShare: Bool = false
    @State var isAlert: Bool = false
    private let titles: [String] = ["添加书签","分享"]
    private let imagesNames: [String] = ["bookmark", "share"]
    
    @State private var offsetY: CGFloat = 300
    
    var body: some View {
        
        VStack(spacing: 16) {
            
            ForEach(0..<titles.count, id: \.self) { index in
                Button {
                    if index == 0{
                        addToBookmark()
                    }else if index == 1{
                        isShare.toggle()
                    }
                } label: {
                    MenuCell(title: titles[index], imageName: imagesNames[index])
                }
                .sheet(isPresented: $isShare) {
                    ActivityViewController(urlString: emptyLink)
                }
            }
            
            Toggle(isOn: $isTraceless ) {
                Text("无痕模式")
                    .padding(16)
                    .foregroundColor(Color(hexString: "0A1626"))
                    .font(.system(size: 16))
            }
            .onChange(of: isTraceless, perform: { newValue in
                TraceLessMode.shared.isON = newValue
            })
            .toggleStyle(CustomToggleStyle())
            .frame(height: 50)
            .background(.white)
            .cornerRadius(6)
            .padding(.horizontal, 16)
            
            
            if isAlert {
                ZStack {
                    Text("已添加至书签")
                        .frame(width: 150, height: 50)
                        .background(SwiftUI.Color.black.opacity(0.35))
                        .cornerRadius(25)
                        .foregroundColor(.white)
                        .font(.system(size: 15))
                        .offset(y: offsetY)
                }
            }
        }
        .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
        
    }
    
    private func addToBookmark() {
    // TODO: addToBookmark
        let webCache = WebCacheMgr.shared.store[selectedTab.curIndex]
        let manager = BookmarkCoreDataManager()
        let bookmark = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
        let result = manager.insertBookmark(bookmark: bookmark)
        if result {
            alertAnimation()
        }
    }
    
    private func alertAnimation() {
        isAlert.toggle()
        withAnimation {
            offsetY = 100
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            withAnimation {
                offsetY = 300
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.isAlert.toggle()
        }
    }
}

struct MenuView_Previews: PreviewProvider {
    static var previews: some View {
        MenuView()
    }
}


struct CustomToggleStyle: ToggleStyle {
    
    func makeBody(configuration: Configuration) -> some View {
        Toggle(configuration)
            .padding(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 12))
    }
}

struct ActivityViewController: UIViewControllerRepresentable {
    
    var urlString: String

    func makeUIViewController(context: Context) -> UIViewController {
        
        guard let url = URL(string: urlString) else { return UIViewController() }
        let activityViewController : UIActivityViewController = UIActivityViewController(
            activityItems: [url], applicationActivities: nil)
        
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
        activityViewController.isModalInPresentation = true

        return activityViewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

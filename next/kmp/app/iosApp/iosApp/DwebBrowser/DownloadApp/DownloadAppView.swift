//
//  DownloadAppView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/25.
//

import Combine
import SwiftUI

struct DownloadAppView: View {
    var modelData: Data
    var callback: onStringCallBack
    
    @State var offset: CGPoint = .zero
    @ObservedObject var viewModel = DownloadImageViewModel()
    
    @State private var content: String = "获取"
    @State private var btn_width: CGFloat = 80
    @State private var backColor: SwiftUI.Color = .blue
    @State private var isRotate = false
    @State private var isWaiting = false
    @State private var isLoading = false
    @State private var progress: CGFloat = 0.0
    @State private var defaultManifest: JmmAppDownloadManifest?
    @State private var isPresented = false
    @State private var currentImageIndex: Int = 0
    
    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            navigationView()
                .frame(maxWidth: .infinity)
                .background(.white)
            
            CustomUIScrollView(content: {
                VStack(spacing: 0) {
                    //                    HeaderView()
                    
                    AppTitleView()
                    
                    Divider().frame(height: 0.5).background(.white.opacity(0.35))
                        .padding(.bottom, 20)
                        .padding(.horizontal, 16)
                    
                    AppHistoryDataView()
                    
                    Divider().frame(height: 0.5).background(.white.opacity(0.35))
                        .padding(.vertical, 20)
                        .padding(.horizontal, 16)
                    
                    AppImagesView()
                    
                    Divider().frame(height: 0.5).background(.white.opacity(0.35))
                        .padding(.vertical, 20)
                        .padding(.horizontal, 16)
                    
                    AppIntroductionView()
                    
                    Divider().frame(height: 0.5).background(.white.opacity(0.35))
                        .padding(.vertical, 20)
                        .padding(.horizontal, 16)
                    
                    InfoDataView()
                    
                    Spacer()
                }
            }, offset: $offset, showIndication: false, axis: .vertical)
        }
        .preferredColorScheme(.light)
        .onReceive(progressPublisher) { out in
            progress = CGFloat(out)
        }
        .onAppear {
            loadAppInfo()

            switch defaultManifest?.download_status {
            case .Installed:
                content = "打开"
            case .NewVersion:
                content = "更新"
            default:
                break
            }
        }
        .sheet(isPresented: $isPresented) {
            presentImageController(index: currentImageIndex)
        }
    }
    
    @ViewBuilder
    func navigationView() -> some View {
        ZStack(alignment: .center) {
            HStack(alignment: .center) {
                Button {
                    callback("back")
                } label: {
                    HStack {
                        Image(systemName: "chevron.left")
                        Text("返回")
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical)
                }
                
                Spacer()
                
                DownloadButtonView(content: $content, btn_width: $btn_width, backColor: $backColor, isRotate: $isRotate, isWaiting: $isWaiting, isLoading: $isLoading, progress: $progress, callback: callback)
                    .padding(.trailing, 20)
                    .padding(.bottom, 5)
                    .opacity(calculateNavigationViewSubViewAlpha())
            }
            Image(uiImage: viewModel.iconImage ?? UIImage.assetsImage(name: "360so"))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 40, height: 40)
                .background(.white)
                .cornerRadius(20)
                .padding(.bottom, 10)
                .opacity(calculateNavigationViewSubViewAlpha())
        }
    }
    
    @ViewBuilder
    func HeaderView() -> some View {
        GeometryReader { proxy in
            let minY = proxy.frame(in: .named("SCROLL")).minY
            let size = proxy.size
            let height = size.height + minY
            
            Image(uiImage: UIImage.assetsImage(name: "dweb_icon"))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: size.width, height: height > 0 ? height : 150, alignment: .top)
                .cornerRadius(15)
                .offset(y: -minY)
        }
        .frame(height: 200)
    }
    
    @ViewBuilder
    func AppTitleView() -> some View {
        HStack(alignment: .top) {
            Image(uiImage: viewModel.iconImage ?? UIImage.assetsImage(name: "360so"))
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 80, height: 80)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .background(.white)
                .cornerRadius(20)
            
            VStack(alignment: .leading, spacing: 0) {
                Text(defaultManifest?.name ?? "")
                    .font(.system(size: 20, weight: .bold))
                Text(defaultManifest?.short_name ?? "")
                    .font(.system(size: 13))
                    .foregroundColor(.primary.opacity(0.5))
                Spacer()
                DownloadButtonView(content: $content, btn_width: $btn_width, backColor: $backColor, isRotate: $isRotate, isWaiting: $isWaiting, isLoading: $isLoading, progress: $progress, callback: callback)
            }
            .padding(.leading, 10)
            
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 20)
    }
    
    @ViewBuilder
    func AppImagesView() -> some View {
        VStack {
            Text("预览")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 20)
            
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 20) {
                    ForEach(viewModel.images.indices, id: \.self) { index in
                        let image = viewModel.images[index]
                        Image(uiImage: image)
                            .resizable()
                            .frame(width: 220, height: 350)
                            .cornerRadius(20)
                            .onTapGesture {
                                self.currentImageIndex = index
                                self.isPresented.toggle()
                            }
                    }
                }
            }
            .padding(.horizontal, 10)
        }
    }
    
    @ViewBuilder
    func presentImageController(index: Int) -> some View {
        GeometryReader { geometry in
            VStack(alignment: .trailing) {
                Button {
                    self.isPresented = false
                } label: {
                    Text("完成")
                }
                .padding(.trailing, 20)
                .padding(.top, 20)

                Spacer()
                
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 20) {
                            ForEach(viewModel.images.indices, id: \.self) { index in
                                let image = viewModel.images[index]
                                Image(uiImage: image)
                                    .resizable()
                                    .frame(width: geometry.size.width - 30)
                                    .cornerRadius(20)
                            }
                        }
                        .onAppear {
                            proxy.scrollTo(currentImageIndex)
                        }
                    }
                    .padding(.horizontal, 10)
                }
                
                Spacer()
            }
        }
    }
    
    @ViewBuilder
    func AppHistoryDataView() -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("新功能")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
            
            Text("版本 \(defaultManifest?.version ?? "")")
                .font(.system(size: 16))
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 10)
            
//            ForEach(defaultManifest?.new_feature ?? [], id: \.self) { content in
//                Text("- \(content)")
//            }
        }
        .padding(.leading, 20)
    }
    
    @ViewBuilder
    func AppIntroductionView() -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("简介")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 10)
            Text(defaultManifest?.description ?? "")
        }
        .padding(.leading, 20)
    }
    
    @ViewBuilder
    func InfoDataView() -> some View {
        let titles = ["销售商", "大小"]
        let contents = [defaultManifest?.author.joined(separator: ", ") ?? "", calculateAppSize()]
        VStack(alignment: .leading, spacing: 10) {
            Text("信息")
                .font(.title2.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.bottom, 10)
            
            ForEach(0 ..< titles.count, id: \.self) { index in
                
                HStack {
                    Text(titles[index])
                    Spacer()
                    Text(contents[index])
                }
                .padding(.vertical, 10)
            }
        }
        .padding(.horizontal, 20)
    }
    
    private func calculateNavigationViewAlpha() -> CGFloat {
        if offset.y <= 0 {
            return 0.0
        }
        if offset.y >= 100 {
            return 1.0
        }
        return offset.y / 100
    }
    
    private func calculateNavigationViewSubViewAlpha() -> CGFloat {
        if offset.y >= 100 {
            return 1.0
        }
        return 0
    }
    
    private func calculateAppSize() -> String {
        guard defaultManifest != nil else { return "" }
        
        if defaultManifest!.bundle_size > 1024 * 1024 {
            return String(format: "%.2f MB", CGFloat(defaultManifest!.bundle_size) / (1024.0 * 1024.0))
        }
        if defaultManifest!.bundle_size > 1024 {
            return String(format: "%.2f KB", CGFloat(defaultManifest!.bundle_size) / 1024.0)
        }
        return "\(defaultManifest!.bundle_size)B"
    }
    
    private func statusBarHeight() -> CGFloat {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return 0 }
        return scene.statusBarManager?.statusBarFrame.height ?? 0
    }
    
    private func loadAppInfo() {
        do {
            let decoder = JSONDecoder()
            defaultManifest = try decoder.decode(JmmAppDownloadManifest.self, from: modelData)
            viewModel.loadIcon(urlString: defaultManifest?.logo ?? "", placeHoldImageName: "360so")
            viewModel.loadImages(imageNames: defaultManifest?.images ?? [], placeHoldImageName: "dweb_icon")
        } catch {
            fatalError("could load fail. \n\(error.localizedDescription)")
        }
    }
}

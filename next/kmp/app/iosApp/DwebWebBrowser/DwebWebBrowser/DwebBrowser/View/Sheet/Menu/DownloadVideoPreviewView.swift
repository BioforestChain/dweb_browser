//
//  DownloadVideoPreviewVIew.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/18.
//

import SwiftUI
import AVKit
import Observation

public struct DownloadVideoPreviewView: View {
    let url: URL
    public init(url: URL) {
        self.url = url
    }
    
    @Environment(\.dismiss) private var dismiss
    public var body: some View {
        DownloadVideoPreviewImpView(viewModel: DownloadVideoPreviewImpViewModel(url: url))
            .navigationBarBackButtonHidden()
            .toolbar {
                ToolbarItem(placement: .navigation) {
                    Button(action: {
                        dismiss()
                    }, label: {
                        Image(systemName: "chevron.backward")
                            .foregroundColor(.white)
                    })
                }
            }
    }
}

@Observable class DownloadVideoPreviewImpViewModel: NSObject {
    var player: AVPlayer = AVPlayer()
    var isPlaying: Bool = false
    var fullScreen = false
    
    var timeInfoString = "00:00"
    @ObservationIgnored
    var duration: Float = 0 {
        didSet {
            timeInfoString = getTimeInfoString(current, duration)
        }
    }
    @ObservationIgnored
    var current: Float = 0 {
        didSet {
            timeInfoString = getTimeInfoString(current, duration)
        }
    }
    
    var progress: Float = 0.0
    @ObservationIgnored
    var token: Any? = nil
    var hideControlBar = false
    var isReady = false
    
    @ObservationIgnored
    private var isNeedAutoPlay = false
    
    init(url: URL) {
        player = AVPlayer(url: url)
    }

    func play() {
        player.play()
        isPlaying = true
        resetAutoHideTimer()
    }
    
    func pause() {
        player.pause()
        isPlaying = false
        stopAutoHideTimer()
    }
    
    private func resetAutoHideTimer() {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(hideControllBarAction), object: nil)
        perform(#selector(hideControllBarAction), with: nil, afterDelay: 3)
    }
    
    private func stopAutoHideTimer() {
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(hideControllBarAction), object: nil)
    }
    
    @objc private func hideControllBarAction() {
        hideControlBar = true
    }
    
    func playToggle() {
        if player.rate > 0 {
            pause()
        } else {
            play()
        }
    }
    
    func touchScreenToggle() {
        hideControlBar.toggle()
        if hideControlBar {
            stopAutoHideTimer()
        } else {
            resetAutoHideTimer()
        }
    }
    
    func seek(_ editing: Bool) {
        Log("\(editing)")
        if editing && player.rate > 0 {
            pause()
            isNeedAutoPlay = true
        } else if !editing {
            Log("\(CMTime(value: Int64(duration * progress), timescale: 1).seconds)")
            player.seek(to: CMTime(value: Int64(duration * progress), timescale: 1),
                        toleranceBefore: CMTime(value: 1, timescale: 10),
                        toleranceAfter: CMTime(value: 1, timescale: 10))
            if isNeedAutoPlay {
                play()
                isNeedAutoPlay = false
            }
        }
    }
    
    func fullScreenAction() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
        windowScene.requestGeometryUpdate(.iOS(interfaceOrientations: fullScreen ? .portrait : .landscape))
        fullScreen.toggle()
    }
    
    func prepareVideo() async {
        let dur = (try? await player.currentItem?.asset.load(.duration).seconds) ?? 0
        duration = Float(dur)
        
        token = player.addPeriodicTimeObserver(forInterval: CMTime(value: 1, timescale: 4), queue: DispatchQueue.main) { time in
            self.current = Float(time.seconds)
            if self.duration > 0 {
                self.progress = self.current / self.duration
            } else {
                self.progress = 0.0
            }
        }
        
        isReady = true
    }
    
    func getTimeInfoString(_ cur: Float, _ dur: Float) -> String {
        return getTimeString(Int(cur)) + "/" + getTimeString(Int(dur))
    }
    
    func getTimeString(_ seconds: Int) -> String {
        var hms: (Int, Int, Int) = (0, 0, 0)
        hms.0 = seconds / 3600
        hms.1 = (seconds - hms.0 * 60) / 60
        hms.2 = seconds - hms.0 * 3600 - hms.1 * 60
        
        if hms.0 > 0 {
            return String(format: "%02d:%02d:%02d", hms.0, hms.1, hms.2)
        } else {
            return String(format: "%02d:%02d", hms.1, hms.2)
        }
    }
    
    deinit {
        if let token = token {
            player.removeTimeObserver(token)
        }
    }
}

struct DownloadVideoPreviewImpView: View {
    
    @Bindable var viewModel: DownloadVideoPreviewImpViewModel
    
    var body: some View {
        GeometryReader(content: { geometry in
            let _ = Log("\(geometry.size) \(geometry.safeAreaInsets)")
            ZStack(alignment: .center) {
                if viewModel.isReady {
                    ZStack {
                        Rectangle()
                            .onTapGesture {
                                viewModel.touchScreenToggle()
                            }
                        VideoPlayer(player: viewModel.player)
                            .disabled(true)

                    }
                    
                    VStack {
                        Spacer()
                        controllBar
                            .background {
                                Color
                                    .white
                                    .ignoresSafeArea()
                            }
                            .animation(.easeInOut, value: viewModel.hideControlBar)
                            .offset(y: viewModel.hideControlBar ? 100 : 0)
                            .disabled(!viewModel.isReady)
                    }
                } else {
                    Group {
                        Color.black
                        Text("Loading...")
                            .foregroundStyle(.white)
                            .background {
                                Color.gray
                                    .frame(minWidth: 100, minHeight: 100)
                                    .cornerRadius(10)
                            }
                    }
                    
                }
            }
            .task {
                await viewModel.prepareVideo()
            }
        })
        .background {
            Color.black
                .ignoresSafeArea()
        }
    }
    
    var controllBar: some View {
        HStack(content: {
            Button {
                viewModel.playToggle()
            } label: {
                Image(systemName: viewModel.isPlaying ? "pause" : "play")
                    .frame(width: 20)
            }
            .padding(15)
            
            Spacer()
            
            Slider(value: $viewModel.progress, in: 0...1.0) { editing in
                viewModel.seek(editing)
            }

            Spacer()

            Text(viewModel.timeInfoString)
            Spacer()

            Button("", systemImage: viewModel.fullScreen ? "arrow.down.forward.and.arrow.up.backward.square" : "arrow.up.left.and.arrow.down.right.square") {
                viewModel.fullScreenAction()
            }
            .font(.system(size: 24))
            .padding(15)
        })
        .tint(.black)
        .background {
            Color.white
        }
    }
}


#Preview(body: {
    VStack {
        DownloadVideoPreviewView(url: URL(string: "https://file-examples.com/storage/fef545ae0b661d470abe676/2017/04/file_example_MP4_480_1_5MG.mp4")!)
    }
})

//
//  DownloadAudioPreviewView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/18.
//

import SwiftUI
import AVKit
import Observation

public struct DownloadAudioPreviewView: DownloadPreview {
    let url: URL
    
    init(_ url: URL) {
        self.url = url
    }
    
    static func isSupport(_ mime: DownloadDataMIME, _ localPath: String?) -> Bool {
        if case .audio(_) = mime, let _ = localPath {
            return true
        } else {
            return false
        }
    }
    
    public var body: some View {
        DownloadAudioPreviewImpView(viewModel: DownloadAudioPreviewViewModel(audioUrl: url))
    }
}

@Observable class DownloadAudioPreviewViewModel: NSObject, AVAudioPlayerDelegate {
    var player = AVAudioPlayer()
    var progress: CGFloat = 0.0
    
    @ObservationIgnored
    let audioUrl: URL
        
    var isPlaying: Bool = false
    
    @ObservationIgnored
    private var isFinish = false
    
    @ObservationIgnored
    private var timer: CADisplayLink?
    
    private func startTimer() {        
        timer = CADisplayLink(target: self, selector: #selector(updateProgress))
        timer?.preferredFrameRateRange = .init(minimum: 1, maximum: 60, preferred: 5)
        timer?.add(to: RunLoop.main, forMode: .common)
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
    
    @objc private func updateProgress() {
        guard player.duration > 0, !isFinish else {
            stopTimer()
            return
        }
        withAnimation {
            progress = CGFloat(player.currentTime / player.duration)
        }
    }
    
    init(audioUrl: URL) {
        self.audioUrl = audioUrl
        super.init()
        self.createPlayer()
    }
    
    private func createPlayer() {
        try? player = AVAudioPlayer(contentsOf: audioUrl)
        player.prepareToPlay()
        player.delegate = self
    }
        
    func play() {
        if isFinish {
            progress = 0.0
        }
        player.play()
        isFinish = false
        startTimer()
        updateIsPlaying()
    }
    
    func pause() {
        player.pause()
        stopTimer()
        updateIsPlaying()
    }
    
    func playToggle() {
        if player.isPlaying {
            pause()
        } else {
            play()
        }
    }
    
    func seek(to p: CGFloat) {
        Log("seek to:\(p)")
        player.currentTime = TimeInterval(p * player.duration)
        progress = p
    }
    
    private func updateIsPlaying() {
        isPlaying = player.isPlaying
    }
    
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        isFinish = true
        progress = 1.0
        updateIsPlaying()
    }
    
    deinit {
        player.pause()
        stopTimer()
    }
}

struct DownloadAudioPreviewImpView: View {
    
    @Bindable var viewModel: DownloadAudioPreviewViewModel
    init(viewModel: DownloadAudioPreviewViewModel) {
        self.viewModel = viewModel
    }
    
    var body: some View {
        VStack {
            Spacer()
            iconImageView
            Spacer()
            pcmView
            controllBar
        }
    }
    
    var pcmView: some View {
        DownloadAudioPCMWaveSlider(audioUrl: viewModel.audioUrl,
                                   isPlaying: Binding(get: {
            viewModel.isPlaying
        }, set: { v in
            if v {
                viewModel.play()
            } else {
                viewModel.pause()
            }
        }), progress: Binding(get: {
            viewModel.progress
        }, set: { v in
            viewModel.seek(to: v)
        }))
        .frame(height: 100)
    }
    
    var iconImageView: some View {
        Image(systemName: "music.note")
            .resizable()
            .aspectRatio(0.8, contentMode: .fit)
            .frame(width: 100, height: 100)
            .padding(30)
            .background {
                Color.gray.opacity(0.5)
            }
            .cornerRadius(10)
    }
    
    var controllBar: some View {
        HStack {
            ShareLink(item: viewModel.audioUrl) {
                Image(systemName: "square.and.arrow.up")
            }
            .padding(.leading)
            
            Spacer()
            
            Button("", systemImage: viewModel.isPlaying ? "pause.fill" : "play.fill") {
                viewModel.playToggle()
            }
            .padding(.trailing)
        }
        .font(.system(size:26))
    }
}

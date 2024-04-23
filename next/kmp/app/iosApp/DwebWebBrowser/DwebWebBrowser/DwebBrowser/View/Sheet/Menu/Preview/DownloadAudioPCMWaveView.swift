//
//  DownloadAudioPCMWaveView.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/4/17.
//

import SwiftUI
import Observation
import AVFoundation


struct DownloadAudioPCMWaveData: Identifiable {
    let value: CGFloat
    let id: Int
}

struct DownloadAudioPCMWaveView: View {
    @State var pcmDatas = [DownloadAudioPCMWaveData]()
    @State var maxPCM: CGFloat = 1.0
    let audioUrl: URL
    init(_ audioUrl: URL) {
        self.audioUrl = audioUrl
    }
    
    let spaceX: CGFloat = 2
    let lineWidth: CGFloat = 5
    var width: CGFloat {
        return (spaceX + lineWidth) * CGFloat(pcmDatas.count)
    }

    var body: some View {
        GeometryReader(content: { geo in
            let maxHeigh = geo.size.height / 2.0
            let midY = geo.size.height / 2.0
            let fatory = maxHeigh / maxPCM
            LazyHStack(spacing: spaceX) {
                let _ = Log("width \(width)")
                ForEach(pcmDatas, id: \.id) { element in
                    Path({ path in
                        path.move(to: CGPoint(x: 0, y: midY))
                        path.addLine(to: CGPoint(x: 0, y: midY - element.value * fatory))
                    })
                    .stroke(Color.black, lineWidth: lineWidth)
                    .frame(width: lineWidth, height: geo.size.height)
                }
            }
        })
        .frame(width: width)
        .task {
            Task(priority: .background) {
                await load()
            }
        }
    }
        
    func load() async {
        guard let file = try? AVAudioFile(forReading: audioUrl) else {
            return
        }
        let audioFormat = file.processingFormat
        let audioFrameCount = UInt32(file.length)
        let audioRate = file.fileFormat.sampleRate
        
        guard let buffer = AVAudioPCMBuffer(pcmFormat: audioFormat, frameCapacity: audioFrameCount) else {
            return
        }
        
        do {
            try file.read(into: buffer)
            let floatArray = UnsafeBufferPointer(start: buffer.floatChannelData![0], count: Int(buffer.frameLength))
            Log("floatArray: \(floatArray.count)")
            
            var index = 0
            let space = Int(audioRate / 10) //每1秒取10帧数据
            
            while index * space < floatArray.count {
                autoreleasepool {
                    pcmDatas.append(DownloadAudioPCMWaveData(value: CGFloat(floatArray[index * space]), id: index))
                    index += 1
                }
            }
            
            maxPCM = pcmDatas.max(by: { abs($0.value) < abs($1.value) })?.value ?? 1.0
            
            Log("load PCM:\(pcmDatas.count) max:\(maxPCM)")
            
        } catch {
            assert(false, "Error: read file error: \(error)")
        }
    }
}

public struct DownloadAudioPCMWaveSlider: View {
    
    let audioUrl: URL
    @Binding var isPlaying: Bool
    @Binding var progress: CGFloat

    public init(audioUrl: URL, isPlaying: Binding<Bool>, progress: Binding<CGFloat>) {
        self.audioUrl = audioUrl
        self._isPlaying = isPlaying
        self._progress = progress
    }
    
    public var body: some View {
        GeometryReader(content: { geometry in
            ZStack {
                Color.gray.opacity(0.3)
                DownloadAudioPCMScrollView(Color.gray.opacity(0.3),
                                           progress: $progress,
                                           content: {
                    DownloadAudioPCMWaveView(audioUrl)
                }, onDrag: {
                    isPlaying = false
                }, endDrag: { p in
                    progress = CGFloat(p)
                    Log("\(p)")
                })
                indicator
            }
        })
    }

    var indicator: some View {
        Color.white
            .frame(width: 4)
            .cornerRadius(2)
            .overlay {
                RoundedRectangle(cornerRadius: 2)
                    .stroke(Color.gray, lineWidth: 0.5)
            }
    }
}

//
//  DwebConsoleView.swift
//  DwebBrowser
//
//  Created by instinct on 2024/3/11.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Observation

let gologStore = DwebConsoleDataStore()

@Observable class DwebConsoleDataStore {
    
    var logs = [String]()
    
    init() {
        NotificationCenter.default.addObserver(self, selector: #selector(notficationHandle(_:)), name: DwebConsoleDataStore.consoleNotificationName, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc func notficationHandle(_ noc: Notification) {
        guard let obj = noc.object as? String else { return }
        logs.append(obj)
    }
    
    static let consoleNotificationName = NSNotification.Name(rawValue: "DwebConsoleDataStoreNotification")
    
    class func log(_ msg: String?) {
        guard let msg = msg else { return }
        post(msg)
    }
    
    class private func post(_ msg: String) {
        NotificationCenter.default.post(name: consoleNotificationName, object: msg)
    }
    
    static var sourceTimer: DispatchSourceFileSystemObject!

    
    class func listern() {
        
//        let path = NSTemporaryDirectory() + "/log.txt"
//        if FileManager.default.fileExists(atPath: path) {
//            try? FileManager.default.removeItem(atPath: path)
//        }
//        FileManager.default.createFile(atPath: path, contents: nil)
//        
//        
//        let directoryURL = URL(fileURLWithPath: path)
//                
//                let content:[CChar] = directoryURL.path.cString(using: .utf8)!
//                let fd =  open(content, O_EVTONLY)
//                if fd < 0 {
//                    Log("Unable to open the path = \(directoryURL.path)")
//                    return
//                }
//
//                let timer = DispatchSource.makeFileSystemObjectSource(fileDescriptor: fd, eventMask: .write, queue: DispatchQueue.global())
//                timer.setEventHandler {
//                    let event:DispatchSource.FileSystemEvent = timer.data
//                    switch event {
//                    
//                    case .write:
//                        DwebConsoleDataStore.post("Document file write")
//                    default:
//                        DwebConsoleDataStore.post("Document file changed")
//                    }
//                    
//                    
////                    DispatchQueue.main.async {
////                        self?.listTb.reloadData()
////                    }
//                    
//                }
//
//                timer.setCancelHandler {
//                    Log("destroy timer")
//                    close(fd)
//                }
//                
//                sourceTimer = timer
//                timer.resume()
//        
//        freopen(path, "w+", stdout)

//        Task {
//            let (bytes, response) = try await URLSession.shared.bytes(from: URL(fileURLWithPath: path))
//            for try await line in bytes.lines {
//                log("\(line)")
//            }
//        }
    }
}

struct DwebConsoleView: View {
    
    let logStroe = gologStore
    
    @State private var expand: Bool = false
    
    var body: some View {
        ZStack {
            if expand {
                Color.black.ignoresSafeArea()
                ScrollView {
                    LazyVStack(alignment: .leading, content: {
                        
                        ForEach(0..<logStroe.logs.count) { i in
                            let log = logStroe.logs[i]
                            Text("\(i): " + log)
                                .foregroundStyle(.green)
                            Divider()
                                .background {
                                    Color.gray
                                }
                        }
                    })
                }.clipped()
                ZStack(alignment: .bottomTrailing) {
                    Color.clear
                    Button(action: {
                        expand.toggle()
                    }, label: {
                        Image(systemName: "stop.circle")
                            .font(.largeTitle)
                            .foregroundStyle(.red)
                    })
                }
            } else {
                ZStack(alignment: .bottomTrailing) {
                    Color.clear
                    Button(action: {
                        expand.toggle()
                    }, label: {
                        Image(systemName: "record.circle")
                            .font(.largeTitle)
                            .foregroundStyle(.black)
                    })
                }
            }
        }
    }
}

extension Sequence {
    func asyncForEach(_ operation: (Element) async -> Void) async {
        for element in self {
            await operation(element)
        }
    }
}

#Preview {
    DwebConsoleView()
}

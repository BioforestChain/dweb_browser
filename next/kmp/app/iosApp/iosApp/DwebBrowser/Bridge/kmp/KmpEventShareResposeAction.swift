//
//  KmpEventShareResposeAction.swift
//  iosApp
//
//  Created by instinct on 2023/11/3.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit

class KmpEventShareResposeAction: KmpEventResposeAction {
    
    override func parseInputDatas() -> [Any]? {
        var result = [Any]()
        
        if let title = event.inputDatas?["title"] {
            result.append(title)
        }
        if let text = event.inputDatas?["text"] {
            result.append(text)
        }
        if let url = event.inputDatas?["url"] {
            result.append(url)
        }
        if let files = event.inputDatas?["files"] as? [Data]{
            result.append(contentsOf: files)
        }
        return result
    }

    @MainActor
    override func doAction() async -> Any? {
        let controller = UIActivityViewController(activityItems: parseInputDatas() ?? [], applicationActivities: nil)
        UIApplication.shared.keyWindow?.rootViewController!.present(controller, animated: true, completion: nil)
        return await withCheckedContinuation { continuation  in
            controller.completionWithItemsHandler = { _, completed, _, error in
                continuation.resume(returning: completed ? "OK" : (error?.localizedDescription ?? "Cancel"))
            }
        }
    }
}

//    typealias KmpStringUnitType = @convention(block) (String) -> Void
//
//    func outputDatas() -> ((String)->Void)? {
//        guard let event = KmpBridgeManager.shared.event(for: KmpEvent.share) else {
//            return nil
//        }
//        guard let callback = event.outputDatas?["callback"] else { return nil }
//        // FIXME: 此处比较危险，直接避开编译器的安全检查，直接强转为swift的closure. 后期看情况是否需要优化
//        let handler = unsafeBitCast(callback as AnyObject, to: KmpStringUnitType.self)
//        return handler
//    }

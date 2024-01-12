//
//  HapticsHelper.swift
//  DwebBrowser
//
//  Created by bfs-kingsword09 on 2023/6/30.
//

import AudioToolbox
import CoreHaptics

// 用于修复 dotnet 中的 CoreHaptics 功能不完整
@objc(HapticsHelper)
public class HapticsHelper: NSObject {
    @objc public static func vibrate(durationArr: [Double]) {
        if CHHapticEngine.capabilitiesForHardware().supportsHaptics {
            do {
                let engine = try CHHapticEngine()
                try engine.start()
                engine.resetHandler = {
                    do {
                        try engine.start()
                    } catch {
                        AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
                    }
                }

                var events: [CHHapticEvent] = []
                var relativeTime = 0.0

                durationArr.enumerated().forEach { index, duration in
                    if index % 2 == 0 {
                        let intensity = CHHapticEventParameter(parameterID: .hapticIntensity, value: 0.5)
                        let sharpness = CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.6)
                        let continuousEvent = CHHapticEvent(eventType: .hapticContinuous, parameters: [intensity, sharpness], relativeTime: relativeTime, duration: max(0.01, Double(duration)/1000))
                        events.append(continuousEvent)
                    }

                    relativeTime += Double(duration)/1000
                }

                let pattern = try CHHapticPattern(events: events, parameters: [])
                let player = try engine.makePlayer(with: pattern)
                try player.start(atTime: 0)

            } catch {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        } else {
            AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
        }
    }
}

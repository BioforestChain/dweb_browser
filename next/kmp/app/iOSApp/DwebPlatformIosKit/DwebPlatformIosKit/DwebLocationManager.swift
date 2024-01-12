//
//  DwebLocationHelper.swift
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/12/4.
//

import UIKit
import CoreLocation

@objc public class DwebLocationRequestApi: NSObject {
    
    @objc public func requestLocation(completed: @escaping (CLLocation?, Int, String?)->Void) {
        let req = DwebLocationTaskRequest.location(completed: completed)
        let task = DwebLocationTask(request: req)
        DwebLocationManager.shared.doAddTask(task)
    }
    
    @objc public func requestTrack(_ mmid: String,  fps: Int = 10, updated: @escaping (CLLocation?, Int ,String?)->Void) {
        let req = DwebLocationTaskRequest.track(fps: fps, mmid: mmid, updated: updated)
        let task = DwebLocationTask(request: req)
        DwebLocationManager.shared.doAddTask(task)
    }
    
    @objc public func removeTrack(mmid: String) {
        DwebLocationManager.shared.removeTask(mmid)
    }
}

enum DwebLocationTaskRequest {
    case location(completed: (CLLocation?, Int, String?)->Void)
    case track(fps: Int, mmid: String, updated: (CLLocation?, Int, String?)->Void)
    
    var isLocation: Bool {
        if case .location(_) = self {
            return true
        }
        return false
    }
        
    var fps: Int {
        switch self {
        case .location(completed: _):
            return 0 //立即执行
        case let .track(fps: fps, mmid: _, updated: _):
            return DwebLocationManager.timerInterval.contains(fps) ? fps : DwebLocationManager.timerInterval.lowerBound
        }
    }
    
    var mmid: String? {
        switch self {
        case .location(completed: _):
            return nil
        case let .track(fps: _, mmid: mmid, updated: _):
            return mmid
        }
    }
}

class DwebLocationTask {
        
    let request: DwebLocationTaskRequest
    
    private var tiggerCount: Int
        
    init(request: DwebLocationTaskRequest) {
        self.request = request
        self.tiggerCount = request.fps
    }
    
    func consumptionTimerTick(_ location: CLLocation?, code: Int, err: String? = nil, force: Bool = false) -> Bool {
        tiggerCount -= 1
        Log("tiggercount:\(tiggerCount)")
        guard (tiggerCount <= 0) || (force == true) else {
            return true
        }
        tiggerCount = request.fps
        return consumption(location, code: code, err: err)
    }
    
    //是否继续追踪定位
    private func consumption(_ location: CLLocation?, code: Int, err: String? = nil) -> Bool {
        switch request {
        case let .location(completed: completed):
            completed(location, code, err)
            return false
        case let .track(fps:_, mmid: _, updated: updated):
            updated(location, code, err)
            return (err != nil) ? false : true
        }
    }
}

class DwebLocationManager: NSObject {
    
    static let shared = DwebLocationManager()
            
    lazy var locationManager: CLLocationManager = {
       let locationManager = CLLocationManager()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        return locationManager
    }()

    
    private var tasks: [DwebLocationTask] = []
    
    private lazy var currentAuth: CLAuthorizationStatus = {
        locationManager.authorizationStatus
    }()
    
    private var currentLocation: CLLocation? = nil
    private var currentState: (Int, String?) = (0, nil) // 0: 成功, 1:无定位权限, 2:位置不可定位, 3.超时, 4：其他原因
    private var timer: Timer? = nil
    
    fileprivate static let timerInterval: ClosedRange<Int> = 5...10
    private static let userDeniedError = "用户拒绝地理位置授权"

    private func startLocationTimerIfNeed() {
        locationManager.startUpdatingLocation()
        guard timer == nil else { return }
        self.timer = Timer(timeInterval: 1,
                           target: self,
                           selector: #selector(timerTick),
                           userInfo: nil,
                           repeats: true)
        RunLoop.main.add(self.timer!, forMode: .common)
        Log("定位timer创建")
    }
    
    private func stopLocationTimerIfNeed() {
        guard tasks.isEmpty else { return }
        timer?.invalidate()
        timer = nil
        currentLocation = nil
        currentState = (0, nil)
        locationManager.stopUpdatingLocation()
        Log("定位timer销毁")
    }
    
    @objc private func timerTick(_ force: Bool = false) {
        Log("定位timer执行")
        if currentAuth == .notDetermined {
            // skip, waiting for user auth!
            return
        }

        let continueTackTasks = tasks.filter { $0.consumptionTimerTick(currentLocation, code: currentState.0, err: currentState.1, force: force)}
        tasks = continueTackTasks
        stopLocationTimerIfNeed()
    }
    
    fileprivate func doAddTask(_ task: DwebLocationTask) {
        Log("doAddTask")
        var add = true
        if (currentLocation != nil) || (currentState.0 != 0) {
            add = task.consumptionTimerTick(currentLocation, code: currentState.0, err: currentState.1, force: true)
        }
        
        if add {
            Log("doAddTask add")
            DispatchQueue.main.async { [weak self] in
                self?.tasks.append(task)
                self?.actionDependAuth()
            }

        }
    }
    
    fileprivate func removeTask(_ mmid: String) {
        Log("removeTask")
        tasks.removeAll { $0.request.mmid == mmid }
    }
    
    private func actionDependAuth() {
        if currentAuth == .authorizedAlways || currentAuth == .authorizedWhenInUse {
            startLocationTimerIfNeed()
        } else if currentAuth == .notDetermined {

            locationManager.requestWhenInUseAuthorization()

        } else {
            currentState = (1, DwebLocationManager.userDeniedError)
            timerTick()
        }
    }
}

extension DwebLocationManager: CLLocationManagerDelegate {
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let auth = manager.authorizationStatus
        currentAuth = auth
        switch auth {
        case .authorizedAlways, .authorizedWhenInUse:
            Log("allow location")
        case .denied, .restricted:
            Log("reject location")
            currentState = (1, DwebLocationManager.userDeniedError)
        default:
            Log("not determined location")
        }
        actionDependAuth()
    }
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        Log("didUpdateLocations: \(String(describing: locations.last))")
        guard let location = locations.last else { return }
        let actionRightNow = currentLocation == nil
        currentLocation = location
        if actionRightNow {
            timerTick(true)
        }
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Log("err: \(error.localizedDescription)")
        currentState = (4, error.localizedDescription)
        locationManager.stopUpdatingLocation()
    }
}

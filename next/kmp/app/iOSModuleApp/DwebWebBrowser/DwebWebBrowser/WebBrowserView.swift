// The Swift Programming Language
// https://docs.swift.org/swift-book

import UIKit
import SwiftUI
import Foundation
import DwebPlatformIosKit

var browserViewDelegate: WebBrowserViewDelegate {
    return WebBrowserView.delegate ?? WebBrowserDefaultProvider(trackModel: false)
}

var browserViewDataSource: WebBrowserViewDataSource {
    return WebBrowserView.dataSource ?? WebBrowserDefaultProvider(trackModel: false)
}

fileprivate var addWebBrowserViewDataProtocol: Void = {
    Log("addWebBrowserViewDataProtocol")
    if let objClassStr = WebBrowserView.dataSource?.getWebBrowserViewDataClass(), let objC = NSClassFromString(objClassStr) {
        addIfNeedProtocol(protocol: NSProtocolFromString("DwebWebBrowser.WebBrowserViewDataProtocol")! , kmpClass: objC)
    }
}()

fileprivate func addIfNeedProtocol(protocol: Protocol, kmpClass: AnyClass) {
    guard class_conformsToProtocol(kmpClass, `protocol`) == false else { return }
    let result = class_addProtocol(kmpClass, `protocol`)
    Log("\(result)")
}

@objc
public class WebBrowserView: UIView {
    
    @objc public init(frame: CGRect, delegate: WebBrowserViewDelegate?, dataSource: WebBrowserViewDataSource?) {
        super.init(frame: frame)
        WebBrowserView.delegate = delegate
        WebBrowserView.dataSource = dataSource
        _ = addWebBrowserViewDataProtocol
        #if DEBUG
        if let objClassStr = dataSource?.getWebBrowserViewDataClass(), let objC = NSClassFromString(objClassStr) {
            checkObjc(cls: objC)
        }
        #endif
        setupContainerView()
    }
    
    fileprivate static weak var delegate: WebBrowserViewDelegate? = nil
    fileprivate static weak var dataSource: WebBrowserViewDataSource? = nil
    
    deinit {
        Log()
    }
    
    private var hostVC: UIViewController?
    
    lazy var browserContainerView: UIView = {
        let v = UIView(frame: CGRect.zero)
        v.backgroundColor = .yellow
        v.translatesAutoresizingMaskIntoConstraints = false
        return v
    }()
    
    lazy var blurView: UIView = {
        let blurView = UIView(frame: .zero)
        blurView.backgroundColor = .black.withAlphaComponent(0.5)
        return blurView
    }()
    
    var isTransitionEffect = false
    var snap: UIView? = nil
    
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupContainerView() {
        addSubview(browserContainerView)
        NSLayoutConstraint.activate([
            browserContainerView.leadingAnchor.constraint(equalTo: leadingAnchor),
            browserContainerView.trailingAnchor.constraint(equalTo: trailingAnchor),
            browserContainerView.topAnchor.constraint(equalTo: topAnchor),
            browserContainerView.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }
    
    func createBrowser() {
        hostVC = UIHostingController(rootView: BrowserView())
    }
    
    func containerAdd(subView: UIView?) {
        guard let subView = subView else { return }
        subView.translatesAutoresizingMaskIntoConstraints = false
        browserContainerView.addSubview(subView)
        NSLayoutConstraint.activate([
            subView.leadingAnchor.constraint(equalTo: browserContainerView.leadingAnchor),
            subView.trailingAnchor.constraint(equalTo: browserContainerView.trailingAnchor),
            subView.topAnchor.constraint(equalTo: browserContainerView.topAnchor),
            subView.bottomAnchor.constraint(equalTo: browserContainerView.bottomAnchor),
        ])
    }
}

public extension WebBrowserView {
    @objc func doSearch(key: String) {
        BrowserViewStateStore.shared.doSearch(key)
    }
    
    @objc func gobackIfCanDo() -> Bool {
        return BrowserViewStateStore.shared.doBackIfCan()
    }
    
    @objc func browserClear() {
        isTransitionEffect = false
        BrowserViewStateStore.shared.clear()
        browserContainerView.subviews.forEach { $0.removeFromSuperview() }
        hostVC = nil
    }
    
    @objc func browserActive(on: Bool){
        if on == false {
            isTransitionEffect = true
            snap = hostVC?.view.snapshotView(afterScreenUpdates: false)
        }
    }
    
    @objc func prepareToKmp() -> UIView {
        if browserContainerView.subviews.isEmpty {
            createBrowser()
            containerAdd(subView: hostVC?.view)
        }
        
        if isTransitionEffect {
            if let snap = snap {
                containerAdd(subView: snap)
                containerAdd(subView: blurView)
                hostVC?.view.removeFromSuperview()
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                    guard let self = self else { return }
                    UIView.animate(withDuration: 0.3) {
                        self.containerAdd(subView: self.hostVC?.view)
                        snap.removeFromSuperview()
                        self.blurView.alpha = 0.0
                    } completion: { _ in
                        self.blurView.removeFromSuperview()
                        self.blurView.alpha = 1.0
                    }
                }
            }
            isTransitionEffect = false
        }
        
        return browserContainerView
    }
}


// 用于检查数据模型结构，debug有用。
#if DEBUG
func checkObjc(cls: AnyClass) {

     Log("==================\(cls)=======================")
     Log("\(cls) superclass: \(class_getSuperclass(cls))")

     let classType: AnyClass = cls //class_getSuperclass(cls)!
     
     var count: Int32 = 0
     let protocols = class_copyProtocolList(classType, &count)
     Log("\(cls) protocl")
     (0..<Int(count)).forEach { i in
         let cName = protocol_getName(protocols![i])
         let name = String(validatingUTF8: cName)
         Log("\(cls) protocl: \(name ?? "")")
     }
     
     let ivars = class_copyIvarList(classType, &count)
     Log("\(cls) ivar")
     (0..<Int(count)).forEach { i in
         let cName = ivar_getName(ivars![i])
         let name = String(validatingUTF8: cName!)
         Log("\(cls) ivar: \(name ?? "")")
     }
     
     let propertys = class_copyPropertyList(classType, &count)
     Log("\(cls) property")
     (0..<Int(count)).forEach { i in
         let cName = property_getName(propertys![i])
         let name = String(validatingUTF8: cName)
         Log("\(cls) property: \(name ?? "")")
     }
     
     let methods = class_copyMethodList(classType, &count)
     Log("\(cls) method")
     (0..<Int(count)).forEach { i in
         let sel = method_getName(methods![i])
         Log("\(cls) method: \(sel.description)")
     }
             
     Log("=========================================")
 }
#endif

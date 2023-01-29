//
//  AlertEntites.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/23.
//

import Foundation
import SwiftyJSON

extension PlaocHandleModel {
    //alert弹框
    func OpenDialogAlert(param: Any) -> String {
        guard let param = param as? String else { return "" }
        guard let bodyDict = ChangeTools.stringValueDic(param) else { return "" }
        let configString = bodyDict["config"] as? String
        let cbString = bodyDict["cb"] as? String
        let configDict = ChangeTools.stringValueDic(configString ?? "")
        let alertModel = AlertConfiguration(dict: JSON(configDict))
        let alertView = CustomAlertPopView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        alertView.alertModel = alertModel
        alertView.callback = { [weak self] type in
            guard let strongSelf = self else { return }
            guard cbString != nil, cbString!.count > 0 else { return }
            let jsString = cbString! + "(\(true))"
            guard jsString.count > 0 else { return }
            strongSelf.controller?.evaluateJavaScript(jsString: jsString)
        }
        alertView.show()
        return ""
    }
    //prompt弹框
    func OpenDialogPrompt(param: Any) -> String {
        guard let param = param as? String else { return "" }
        guard let bodyDict = ChangeTools.stringValueDic(param) else { return "" }
        let configString = bodyDict["config"] as? String
        let cbString = bodyDict["cb"] as? String
        let configDict = ChangeTools.stringValueDic(configString ?? "")
        let promptModel = PromptConfiguration(dict: JSON(configDict))
        let alertView = CustomPromptPopView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        alertView.promptModel = promptModel
        alertView.callback = { [weak self] type in
            guard let strongSelf = self else { return }
            guard cbString != nil, cbString!.count > 0 else { return }
            var jsString: String = ""
            if type == .confirm {
                jsString = cbString! + "(\"\(alertView.textField.text ?? "")\")"
            } else if type == .cancel {
                jsString = cbString! + "(\(false))"
            }
            guard jsString.count > 0 else { return }
            strongSelf.controller?.evaluateJavaScript(jsString: jsString)
        }
        alertView.show()
        return ""
    }
    //firm弹框
    func OpenDialogConfirm(param: Any) -> String {
        guard let param = param as? String else { return "" }
        guard let bodyDict = ChangeTools.stringValueDic(param) else { return "" }
        let configString = bodyDict["config"] as? String
        let cbString = bodyDict["cb"] as? String
        let configDict = ChangeTools.stringValueDic(configString ?? "")
        let confirmModel = ConfirmConfiguration(dict: JSON(configDict))
        let alertView = CustomConfirmPopView(frame: CGRect(x: 0, y: 0, width: screen_width, height: screen_height))
        alertView.confirmModel = confirmModel
        alertView.callback = { [weak self] type in
            guard let strongSelf = self else { return }
            guard cbString != nil, cbString!.count > 0 else { return }
            var jsString: String = ""
            if type == .confirm {
                jsString = cbString! + "(\(true))"
            } else if type == .cancel {
                jsString = cbString! + "(\(false))"
            }
            guard jsString.count > 0 else { return }
            strongSelf.controller?.evaluateJavaScript(jsString: jsString)
        }
        alertView.show()
        return ""
    }
    //warning弹框
    func OpenDialogWarning(param: Any) -> String {
        return ""
    }
}

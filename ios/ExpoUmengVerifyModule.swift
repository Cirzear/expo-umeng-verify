import ExpoModulesCore
import UMCommon
import UMVerify

public class ExpoUmengVerifyModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoUmengVerify")

    Events("onUMVerifyEvent")

    AsyncFunction("init") { (appKey: String, schemeSecret: String, channel: String, promise: Promise) in
      DispatchQueue.main.async {
        UMConfigure.initWithAppkey(appKey, channel: channel)
        UMCommonHandler.setLogEnabled(true)
        UMCommonLogManager.setUp()
        
        UMVerifyHelper.setAuthSDKInfo(schemeSecret) { result in
            // Initialization callback (optional in some versions, but good to have)
             print("UMVerify init result: \(result ?? [:])")
        }
        promise.resolve(true)
      }
    }

    AsyncFunction("checkEnvAvailable") { (promise: Promise) in
      UMVerifyHelper.checkEnvAvailable(with: .login) { result in
        let isAvailable = result?[UMVerifyConst.resultCode] as? String == UMVerifyConst.successCode
        promise.resolve(isAvailable)
      }
    }

    AsyncFunction("getLoginToken") { (config: [String: Any], promise: Promise) in
      DispatchQueue.main.async {
        let uiConfig = UMCustomModel()
        
        // Basic UI Config mapping (simplified for now, can be expanded)
        if let ui = config["ui"] as? [String: Any] {
            if let navConfig = ui["navigationBar"] as? [String: Any] {
                if let navColor = navConfig["backgroundColor"] as? String {
                    uiConfig.navColor = UIColor(hex: navColor)
                }
                if let title = navConfig["title"] as? String {
                    uiConfig.navTitle = NSAttributedString(string: title)
                }
            }
            
            if let logoConfig = ui["logo"] as? [String: Any] {
                if let imagePath = logoConfig["imagePath"] as? String {
                    uiConfig.logoImage = UIImage(named: imagePath)
                }
                if let hidden = logoConfig["hidden"] as? Bool {
                    uiConfig.logoIsHidden = hidden
                }
            }
             
             // Add more UI mapping as needed...
        }

        UMVerifyHelper.getLoginToken(withTimeout: 5.0, controller: Utilities.currentViewController()!, model: uiConfig) { result in
            let code = result?[UMVerifyConst.resultCode] as? String
            
            if code == UMVerifyConst.successCode {
                 let token = result?[UMVerifyConst.token] as? String
                 // Construct return object matching Android/TS interface
                 let ret = [
                    "code": "600000", // Success code
                    "token": token,
                    "msg": "Success"
                 ]
                 // Serialize to JSON string if that's what Android returns, or object if TS expects object.
                 // Android returns a JSON string in 'ret', so we might need to match that or TS types.
                 // Looking at Android: pendingPromise?.resolve(ret) where ret is the raw string from SDK.
                 // Let's return the raw dictionary or stringify it.
                 // For consistency with Android which returns the raw SDK JSON string:
                 if let jsonData = try? JSONSerialization.data(withJSONObject: result ?? [:], options: []),
                    let jsonString = String(data: jsonData, encoding: .utf8) {
                     promise.resolve(jsonString)
                 } else {
                     promise.resolve("{}")
                 }
                 
                 UMVerifyHelper.quitLoginPage(animated: true, completion: nil)
            } else if code == UMVerifyConst.cancelCode {
                promise.reject("USER_CANCEL", "User cancelled login")
                UMVerifyHelper.quitLoginPage(animated: true, completion: nil)
            } else {
                let msg = result?[UMVerifyConst.resultMsg] as? String ?? "Unknown error"
                promise.reject("TOKEN_ERROR", msg)
                UMVerifyHelper.quitLoginPage(animated: true, completion: nil)
            }
        }
      }
    }
    
    AsyncFunction("accelerateLoginPage") { (promise: Promise) in
        UMVerifyHelper.accelerateLoginPage(withTimeout: 5.0) { result in
             if let jsonData = try? JSONSerialization.data(withJSONObject: result ?? [:], options: []),
                let jsonString = String(data: jsonData, encoding: .utf8) {
                 promise.resolve(jsonString)
             } else {
                 promise.resolve("{}")
             }
        }
    }
    
    AsyncFunction("quitLoginPage") {
        DispatchQueue.main.async {
            UMVerifyHelper.quitLoginPage(animated: true, completion: nil)
        }
    }
  }
}

// Helper extension for Hex color
extension UIColor {
    convenience init?(hex: String) {
        var cString:String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if (cString.hasPrefix("#")) {
            cString.remove(at: cString.startIndex)
        }

        if ((cString.count) != 6) {
            return nil
        }

        var rgbValue:UInt64 = 0
        Scanner(string: cString).scanHexInt64(&rgbValue)

        self.init(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
}

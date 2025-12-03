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
        UMConfigure.setLogEnabled(true)
        
        UMCommonHandler.setVerifySDKInfo(schemeSecret) { result in
             print("UMVerify init result: \(result )")
        }
        promise.resolve(true)
      }
    }

    AsyncFunction("checkEnvAvailable") { (promise: Promise) in
      UMCommonHandler.checkEnvAvailable(with: .loginToken) { result in
        let isAvailable = result?["resultCode"] as? String == "600000"
        promise.resolve(isAvailable)
      }
    }

    AsyncFunction("getLoginToken") { (config: [String: Any], promise: Promise) in
      DispatchQueue.main.async {
        let uiConfig = UMCustomModel()
        
        if let ui = config["ui"] as? [String: Any] {
            if let navConfig = ui["navigationBar"] as? [String: Any] {
                if let navColor = navConfig["backgroundColor"] as? String {
                    uiConfig.navColor = UIColor(hex: navColor) ?? .white
                }
                if let title = navConfig["title"] as? String {
                    uiConfig.navTitle = NSAttributedString(string: title)
                }
            }
            
            if let logoConfig = ui["logo"] as? [String: Any] {
                if let imagePath = logoConfig["imagePath"] as? String,
                   let image = UIImage(named: imagePath) {
                    uiConfig.logoImage = image
                }
                if let hidden = logoConfig["hidden"] as? Bool {
                    uiConfig.logoIsHidden = hidden
                }
            }
        }

        guard let controller = Utilities.currentViewController() else {
            promise.reject("CONTEXT_ERROR", "Current view controller is nil")
            return
        }

        UMCommonHandler.getLoginToken(withTimeout: 5.0, controller: controller, model: uiConfig) { result in
            let code = result["resultCode"] as? String
            
            if code == "600000" {
                 let token = result["token"] as? String
                 
                 if let jsonData = try? JSONSerialization.data(withJSONObject: result, options: []),
                    let jsonString = String(data: jsonData, encoding: .utf8) {
                     promise.resolve(jsonString)
                 } else {
                     promise.resolve("{}")
                 }
                 
                 UMCommonHandler.cancelLoginVCAnimated(true, complete: nil)
            } else if code == "700000" { // User cancelled (back button)
                promise.reject("USER_CANCEL", "User cancelled login")
                UMCommonHandler.cancelLoginVCAnimated(true, complete: nil)
            } else {
                let msg = result["msg"] as? String ?? "Unknown error"
                // Don't reject immediately for some codes if we want to handle them differently, 
                // but for now reject is fine.
                // Note: 600001 is "Wake up success", we shouldn't resolve/reject on that.
                // But getLoginToken callback usually fires with final result or intermediate events?
                // The doc says: "授权页唤起成功" (600001) is also a callback.
                // We should check if it's a final state.
                
                if code == "600001" {
                    // Auth page shown, do nothing, wait for user action
                    return
                }
                
                promise.reject("TOKEN_ERROR", "\(code ?? "Unknown"): \(msg)")
                UMCommonHandler.cancelLoginVCAnimated(true, complete: nil)
            }
        }
      }
    }
    
    AsyncFunction("accelerateLoginPage") { (promise: Promise) in
        UMCommonHandler.accelerateLoginPage(withTimeout: 5.0) { result in
             if let jsonData = try? JSONSerialization.data(withJSONObject: result, options: []),
                let jsonString = String(data: jsonData, encoding: .utf8) {
                 promise.resolve(jsonString)
             } else {
                 promise.resolve("{}")
             }
        }
    }
    
    AsyncFunction("quitLoginPage") {
        DispatchQueue.main.async {
            UMCommonHandler.cancelLoginVCAnimated(true, complete: nil)
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

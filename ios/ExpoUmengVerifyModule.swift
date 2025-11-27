import ExpoModulesCore
import UMCommon
import UMVerify

public class ExpoUmengVerifyModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoUmengVerify")

    AsyncFunction("init") { (appKey: String, channel: String, promise: Promise) in
      UMConfigure.initWithAppkey(appKey, channel: channel)
      UMCommonHandler.setVerifySDKInfo(appKey) { result in
          print("UMVerify init result: \(String(describing: result))")
      }
      promise.resolve(true)
    }

    AsyncFunction("getLoginToken") { (promise: Promise) in
      DispatchQueue.main.async {
        guard let currentController = self.appContext?.utilities?.currentViewController() else {
          promise.reject("ERR_NO_CONTROLLER", "No current view controller found")
          return
        }

        let model = UMCustomModel()
        
        UMCommonHandler.getLoginToken(withTimeout: 3.0, controller: currentController, model: model) { result in
            if let result = result as? [String: Any] {
                let code = result["resultCode"] as? String
                if code == "600000" {
                    promise.resolve(result)
                    UMCommonHandler.cancelLoginVC(animated: true, complete: nil)
                } else {
                    // Don't reject immediately on UI actions like back button (600001? check codes)
                    // But for now, let's resolve with the result and let JS handle it, or reject.
                    // Usually we want to reject on failure.
                    promise.reject("ERR_UMENG_VERIFY", "Code: \(code ?? "unknown") Msg: \(result["msg"] ?? "")")
                    UMCommonHandler.cancelLoginVC(animated: true, complete: nil)
                }
            } else {
                promise.reject("ERR_UNKNOWN", "Unknown result")
                UMCommonHandler.cancelLoginVC(animated: true, complete: nil)
            }
        }
      }
    }
    
    AsyncFunction("accelerateLoginPage") { (promise: Promise) in
        UMCommonHandler.checkEnvAvailable(with: .loginToken) { result in
             if let result = result as? [String: Any] {
                 let code = result["resultCode"] as? String
                 if code == "600000" || code == "600024" { // 600024: Terminal check success
                     promise.resolve(result)
                 } else {
                     promise.reject("ERR_ENV_CHECK", "Code: \(code ?? "unknown")")
                 }
             } else {
                 promise.reject("ERR_UNKNOWN", "Unknown result")
             }
        }
    }
    
    AsyncFunction("quitLoginPage") {
        DispatchQueue.main.async {
            UMCommonHandler.cancelLoginVC(animated: true, complete: nil)
        }
    }
  }
}

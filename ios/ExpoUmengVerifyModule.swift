import ExpoModulesCore

public class ExpoUmengVerifyModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoUmengVerify")

    Function("checkEnvAvailable") { (promise: Promise) in
      // TODO: Implement actual check
      promise.resolve(false)
    }

    Function("getLoginToken") { (config: [String: Any], promise: Promise) in
        // TODO: Implement actual login
        promise.resolve([
            "code": "500",
            "msg": "Not implemented yet",
            "data": nil
        ])
    }
    
    Function("accelerateLoginPage") { (promise: Promise) in
        promise.resolve()
    }
    
    Function("quitLoginPage") {
        // TODO: Implement quit
    }
  }
}


package expo.modules.umengverify

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.umeng.commonsdk.UMConfigure
import com.umeng.umverify.UMVerifyHelper
import com.umeng.umverify.listener.UMTokenResultListener
import com.umeng.umverify.listener.UMPreLoginResultListener
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoUmengVerifyModule : Module() {
  private var verifyHelper: UMVerifyHelper? = null
  private val TAG = "ExpoUmengVerify"
  private var pendingPromise: Promise? = null
  private var currentOp: String = ""

  private fun handleResult(ret: String, success: Boolean) {
      Log.d(TAG, "handleResult: $currentOp, success=$success, ret=$ret")
      if (pendingPromise == null) return

      if (currentOp == "CHECK_ENV") {
          // 600024 means success
          if (ret.contains("600024")) {
              pendingPromise?.resolve(true)
          } else {
              pendingPromise?.resolve(false)
          }
      } else if (currentOp == "LOGIN") {
          if (success) {
              pendingPromise?.resolve(ret)
          } else {
              pendingPromise?.reject("TOKEN_ERROR", ret, null)
          }
          verifyHelper?.quitLoginPage()
      }
      pendingPromise = null
      currentOp = ""
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoUmengVerify")

    AsyncFunction("init") { appKey: String, channel: String, promise: Promise ->
      try {
        val context = appContext.reactContext ?: throw Exception("React Context is null")
        UMConfigure.init(context, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, "")
        
        verifyHelper = UMVerifyHelper.getInstance(context, object : UMTokenResultListener {
            override fun onTokenSuccess(ret: String) {
                handleResult(ret, true)
            }

            override fun onTokenFailed(ret: String) {
                handleResult(ret, false)
            }
        })
        
        verifyHelper?.setAuthSDKInfo(appKey)
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
             Toast.makeText(context, "UMVerify Init Success", Toast.LENGTH_SHORT).show()
        }
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject("INIT_ERROR", e.message, e)
      }
    }

    AsyncFunction("getLoginToken") { promise: Promise ->
      val context = appContext.currentActivity ?: appContext.reactContext
      if (context == null) {
        promise.reject("CONTEXT_ERROR", "Context is null", null)
        return@AsyncFunction
      }
      
      if (verifyHelper == null) {
          promise.reject("INIT_ERROR", "UMVerifyHelper not initialized", null)
          return@AsyncFunction
      }

      Log.d(TAG, "getLoginToken called")
      android.os.Handler(android.os.Looper.getMainLooper()).post {
           Toast.makeText(context, "getLoginToken called", Toast.LENGTH_SHORT).show()
      }

      pendingPromise = promise
      currentOp = "LOGIN"
      
      android.os.Handler(android.os.Looper.getMainLooper()).post {
        verifyHelper?.getLoginToken(context, 5000)
      }
    }

    AsyncFunction("checkEnvAvailable") { promise: Promise ->
        Log.d(TAG, "checkEnvAvailable called")
        val context = appContext.reactContext
        if (context == null) {
            promise.reject("CONTEXT_ERROR", "Context is null", null)
            return@AsyncFunction
        }
        if (verifyHelper == null) {
            promise.reject("INIT_ERROR", "UMVerifyHelper not initialized", null)
            return@AsyncFunction
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
             Toast.makeText(context, "checkEnvAvailable called", Toast.LENGTH_SHORT).show()
             try {
                 pendingPromise = promise
                 currentOp = "CHECK_ENV"
                 // Pass 2 for One-click login check
                 verifyHelper?.checkEnvAvailable(2)
             } catch (e: Exception) {
                 Log.e(TAG, "checkEnvAvailable error", e)
                 promise.reject("CHECK_ENV_ERROR", e.message, e)
                 pendingPromise = null
                 currentOp = ""
             }
        }
    }
    
    AsyncFunction("accelerateLoginPage") { promise: Promise ->
        val context = appContext.reactContext
        if (context == null) {
            promise.reject("CONTEXT_ERROR", "React Context is null", null)
            return@AsyncFunction
        }
        // Assuming accelerateLoginPage takes timeout and listener
        android.os.Handler(android.os.Looper.getMainLooper()).post {
             verifyHelper?.accelerateLoginPage(5000, object : UMPreLoginResultListener {
                override fun onTokenSuccess(ret: String) {
                     promise.resolve(ret)
                }
                override fun onTokenFailed(code: String, msg: String) {
                    promise.reject("ACCELERATE_ERROR", "$code: $msg", null)
                }
            })
        }
    }
    
    AsyncFunction("quitLoginPage") {
        verifyHelper?.quitLoginPage()
    }
  }
}

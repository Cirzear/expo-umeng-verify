
package expo.modules.umengverify

import android.content.Context
import android.util.Log
import android.content.pm.PackageManager

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
      if (pendingPromise == null) {
          Log.w(TAG, "handleResult: pendingPromise is null, ignoring result")
          return
      }

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
      Log.d(TAG, "init called with appKey=$appKey, channel=$channel")
      try {
        val context = appContext.reactContext ?: throw Exception("React Context is null")
        
        // Check permissions
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
             Log.e(TAG, "READ_PHONE_STATE permission not granted!")
        } else {
             Log.d(TAG, "READ_PHONE_STATE permission granted")
        }

        UMConfigure.setLogEnabled(true)
        // Try null for pushSecret
        UMConfigure.init(context, appKey, "Umeng", UMConfigure.DEVICE_TYPE_PHONE, null)
        Log.d(TAG, "UMConfigure.init called")
        
        // Try to use activity context if available, otherwise application context
        val activity = appContext.currentActivity
        val ctx = activity ?: context
        
        verifyHelper = UMVerifyHelper.getInstance(ctx, object : UMTokenResultListener {
            override fun onTokenSuccess(ret: String) {
                Log.d(TAG, "onTokenSuccess: $ret")
                handleResult(ret, true)
            }

            override fun onTokenFailed(ret: String) {
                Log.e(TAG, "onTokenFailed: $ret")
                handleResult(ret, false)
            }
        })
        
        if (verifyHelper == null) {
            Log.e(TAG, "UMVerifyHelper.getInstance returned null")
            throw Exception("Failed to initialize UMVerifyHelper")
        }
        
        // Log version
        // Assuming getVersion() exists, if not, this might fail compilation.
        // But usually SDKs have it. If not sure, skip or try-catch.
        // verifyHelper?.verifySDKVersion might be a static field or method.
        // Let's check imports.
        
        verifyHelper?.setAuthSDKInfo(appKey)
        Log.d(TAG, "setAuthSDKInfo called")

        promise.resolve(true)
      } catch (e: Exception) {
        Log.e(TAG, "init error", e)
        promise.reject("INIT_ERROR", e.message, e)
      }
    }

    AsyncFunction("getLoginToken") { promise: Promise ->
      Log.d(TAG, "getLoginToken called")
      val context = appContext.currentActivity ?: appContext.reactContext
      if (context == null) {
        Log.e(TAG, "getLoginToken: Context is null")
        promise.reject("CONTEXT_ERROR", "Context is null", null)
        return@AsyncFunction
      }
      Log.d(TAG, "getLoginToken context is Activity: ${context is android.app.Activity}")
      
      if (verifyHelper == null) {
          Log.e(TAG, "getLoginToken: UMVerifyHelper not initialized")
          promise.reject("INIT_ERROR", "UMVerifyHelper not initialized", null)
          return@AsyncFunction
      }

      pendingPromise = promise
      currentOp = "LOGIN"
      
      android.os.Handler(android.os.Looper.getMainLooper()).post {
        Log.d(TAG, "calling verifyHelper.getLoginToken")
        verifyHelper?.getLoginToken(context, 5000)
      }
    }

    AsyncFunction("checkEnvAvailable") { promise: Promise ->
        Log.d(TAG, "checkEnvAvailable called")
        val context = appContext.reactContext
        if (context == null) {
            Log.e(TAG, "checkEnvAvailable: Context is null")
            promise.reject("CONTEXT_ERROR", "Context is null", null)
            return@AsyncFunction
        }
        if (verifyHelper == null) {
            Log.e(TAG, "checkEnvAvailable: UMVerifyHelper not initialized")
            promise.reject("INIT_ERROR", "UMVerifyHelper not initialized", null)
            return@AsyncFunction
        }
        

        android.os.Handler(android.os.Looper.getMainLooper()).post {
             try {
                 pendingPromise = promise
                 currentOp = "CHECK_ENV"
                 // Pass 1 for One-click login check (SERVICE_TYPE_LOGIN usually)
                 // Pass 2 for Verification (SERVICE_TYPE_VERIFY)
                 // Let's try 1.
                 Log.d(TAG, "calling verifyHelper.checkEnvAvailable(1)")
                 verifyHelper?.checkEnvAvailable(1)
             } catch (e: Exception) {
                 Log.e(TAG, "checkEnvAvailable error", e)
                 promise.reject("CHECK_ENV_ERROR", e.message, e)
                 pendingPromise = null
                 currentOp = ""
             }
        }
    }
    
    AsyncFunction("accelerateLoginPage") { promise: Promise ->
        Log.d(TAG, "accelerateLoginPage called")
        val context = appContext.reactContext
        if (context == null) {
            promise.reject("CONTEXT_ERROR", "React Context is null", null)
            return@AsyncFunction
        }
        // Assuming accelerateLoginPage takes timeout and listener
        android.os.Handler(android.os.Looper.getMainLooper()).post {
             verifyHelper?.accelerateLoginPage(5000, object : UMPreLoginResultListener {
                override fun onTokenSuccess(ret: String) {
                     Log.d(TAG, "accelerateLoginPage success: $ret")
                     promise.resolve(ret)
                }
                override fun onTokenFailed(code: String, msg: String) {
                    Log.e(TAG, "accelerateLoginPage failed: $code, $msg")
                    promise.reject("ACCELERATE_ERROR", "$code: $msg", null)
                }
            })
        }
    }
    
    AsyncFunction("quitLoginPage") {
        Log.d(TAG, "quitLoginPage called")
        verifyHelper?.quitLoginPage()
    }
  }
}

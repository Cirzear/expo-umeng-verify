
package expo.modules.umengverify

import android.content.Context
import android.util.Log
import com.umeng.commonsdk.UMConfigure
import com.umeng.umverify.UMVerifyHelper
import com.umeng.umverify.listener.UMTokenResultListener
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoUmengVerifyModule : Module() {
  private var verifyHelper: UMVerifyHelper? = null
  private val TAG = "ExpoUmengVerify"

  override fun definition() = ModuleDefinition {
    Name("ExpoUmengVerify")

    AsyncFunction("init") { appKey: String, channel: String, promise: Promise ->
      try {
        val context = appContext.reactContext ?: throw Exception("React Context is null")
        UMConfigure.init(context, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, "")
        verifyHelper = UMVerifyHelper.getInstance(context) { code, msg ->
            Log.d(TAG, "UMVerifyHelper init: code=$code, msg=$msg")
        }
        verifyHelper?.setAuthSDKInfo(appKey)
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject("INIT_ERROR", e.message, e)
      }
    }

    AsyncFunction("getLoginToken") { promise: Promise ->
      val context = appContext.reactContext
      if (context == null) {
        promise.reject("CONTEXT_ERROR", "React Context is null")
        return@AsyncFunction
      }
      
      if (verifyHelper == null) {
          verifyHelper = UMVerifyHelper.getInstance(context) { code, msg ->
            Log.d(TAG, "UMVerifyHelper init: code=$code, msg=$msg")
          }
      }

      verifyHelper?.getLoginToken(context, object : UMTokenResultListener {
        override fun onTokenSuccess(ret: String) {
          Log.d(TAG, "onTokenSuccess: $ret")
          promise.resolve(ret)
          verifyHelper?.quitLoginPage(context)
        }

        override fun onTokenFailed(ret: String) {
          Log.e(TAG, "onTokenFailed: $ret")
          promise.reject("TOKEN_ERROR", ret)
          verifyHelper?.quitLoginPage(context)
        }
      })
    }
    
    AsyncFunction("accelerateLoginPage") { promise: Promise ->
        val context = appContext.reactContext
        if (context == null) {
            promise.reject("CONTEXT_ERROR", "React Context is null")
            return@AsyncFunction
        }
        verifyHelper?.accelerateLoginPage(context, object : UMTokenResultListener {
            override fun onTokenSuccess(ret: String) {
                 promise.resolve(ret)
            }
            override fun onTokenFailed(ret: String) {
                promise.reject("ACCELERATE_ERROR", ret)
            }
        })
    }
    
    AsyncFunction("quitLoginPage") {
        val context = appContext.reactContext
        if (context != null) {
            verifyHelper?.quitLoginPage(context)
        }
    }
  }
}

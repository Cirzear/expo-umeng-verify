package expo.modules.umengverify

import android.content.Context
import android.util.Log
import android.graphics.Color
import android.view.View
import android.content.pm.ActivityInfo
import android.os.Build

import com.umeng.commonsdk.UMConfigure
import com.umeng.umverify.UMVerifyHelper
import com.umeng.umverify.UMResultCode
import com.umeng.umverify.listener.UMTokenResultListener
import com.umeng.umverify.listener.UMPreLoginResultListener
import com.umeng.umverify.listener.UMAuthUIControlClickListener
import com.umeng.umverify.view.UMAuthUIConfig
import com.umeng.umverify.model.UMTokenRet
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import org.json.JSONObject

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

      try {
          val tokenRet = UMTokenRet.fromJson(ret)
          
          if (currentOp == "CHECK_ENV") {
              // Use UMResultCode constant for environment check success
              if (UMResultCode.CODE_ERROR_ENV_CHECK_SUCCESS == tokenRet.code) {
                  Log.d(TAG, "Environment check success")
                  pendingPromise?.resolve(true)
              } else {
                  Log.w(TAG, "Environment check failed: ${tokenRet.code}")
                  pendingPromise?.resolve(false)
              }
          } else if (currentOp == "LOGIN") {
              if (UMResultCode.CODE_START_AUTHPAGE_SUCCESS == tokenRet.code) {
                  Log.i(TAG, "Auth page started successfully: $ret")
                  // Don't resolve yet, wait for token
                  return
              }
              
              if (UMResultCode.CODE_GET_TOKEN_SUCCESS == tokenRet.code) {
                  Log.i(TAG, "Get token success: $ret")
                  pendingPromise?.resolve(ret)
                  verifyHelper?.quitLoginPage()
              } else if (UMResultCode.CODE_ERROR_USER_CANCEL == tokenRet.code) {
                  Log.w(TAG, "User cancelled login")
                  pendingPromise?.reject("USER_CANCEL", "User cancelled login", null)
                  verifyHelper?.quitLoginPage()
              } else {
                  Log.e(TAG, "Get token failed: $ret")
                  pendingPromise?.reject("TOKEN_ERROR", ret, null)
                  verifyHelper?.quitLoginPage()
              }
          }
      } catch (e: Exception) {
          Log.e(TAG, "Error parsing result", e)
          if (currentOp == "CHECK_ENV") {
              pendingPromise?.resolve(false)
          } else {
              pendingPromise?.reject("PARSE_ERROR", "Failed to parse result: ${e.message}", e)
              verifyHelper?.quitLoginPage()
          }
      }
      
      pendingPromise = null
      currentOp = ""
  }

  private fun configAuthPage(context: Context, config: Map<String, Any>?) {
      verifyHelper?.setUIClickListener { code, _, jsonString ->
          try {
              val jsonObj = JSONObject(jsonString)
              when (code) {
                  UMResultCode.CODE_ERROR_USER_CANCEL -> {
                      Log.d(TAG, "User cancelled login (UI)")
                      verifyHelper?.quitLoginPage()
                  }
                  else -> {
                       Log.d(TAG, "UI Event: $code, $jsonString")
                  }
              }
          } catch (e: Exception) {
              Log.e(TAG, "Error parsing UI event", e)
          }
      }

      verifyHelper?.removeAuthRegisterXmlConfig()
      verifyHelper?.removeAuthRegisterViewConfig()

      var authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      if (Build.VERSION.SDK_INT == 26) {
          authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
      }

      val builder = UMAuthUIConfig.Builder()
          .setAppPrivacyColor(Color.GRAY, Color.parseColor("#002E00"))
          .setSwitchAccHidden(true)
          .setLogBtnToastHidden(true)
          .setStatusBarColor(Color.TRANSPARENT)
          .setStatusBarUIFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
          .setLightColor(true)
          .setWebNavTextSizeDp(20)
          .setScreenOrientation(authPageOrientation)

      val privacyOneName = config?.get("privacyOneName") as? String
      val privacyOneUrl = config?.get("privacyOneUrl") as? String
      val privacyTwoName = config?.get("privacyTwoName") as? String
      val privacyTwoUrl = config?.get("privacyTwoUrl") as? String

      if (!privacyOneName.isNullOrEmpty() && !privacyOneUrl.isNullOrEmpty()) {
          builder.setAppPrivacyOne(privacyOneName, privacyOneUrl)
      }
      if (!privacyTwoName.isNullOrEmpty() && !privacyTwoUrl.isNullOrEmpty()) {
          builder.setAppPrivacyTwo(privacyTwoName, privacyTwoUrl)
      }

      verifyHelper?.setAuthUIConfig(builder.create())
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoUmengVerify")

    AsyncFunction("init") { appKey: String, schemeSecret: String, channel: String, promise: Promise ->
      Log.d(TAG, "init called with appKey=$appKey, schemeSecret=$schemeSecret, channel=$channel")
      try {
        val context = appContext.reactContext ?: throw Exception("React Context is null")
        
        // Compliance requirement: preInit before init
        UMConfigure.preInit(context, appKey, channel)
        Log.d(TAG, "UMConfigure.preInit called for compliance")
        
        UMConfigure.setLogEnabled(true)
        // Use appKey for UMConfigure.init
        UMConfigure.init(context, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, null)
        Log.d(TAG, "UMConfigure.init called with appKey")
        
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
        
        // Enable SDK logging as per demo
        verifyHelper?.setLoggerEnable(true)
        Log.d(TAG, "SDK logging enabled")
        
        // Use schemeSecret for setAuthSDKInfo
        verifyHelper?.setAuthSDKInfo(schemeSecret)
        Log.d(TAG, "setAuthSDKInfo called with schemeSecret")

        promise.resolve(true)
      } catch (e: Exception) {
        Log.e(TAG, "init error", e)
        promise.reject("INIT_ERROR", e.message, e)
      }
    }

    AsyncFunction("getLoginToken") { config: Map<String, Any>?, promise: Promise ->
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
        // Configure Auth Page before getting token
        configAuthPage(context, config)

        // Set auth listener before calling getLoginToken as per demo
        val tokenListener = object : UMTokenResultListener {
            override fun onTokenSuccess(ret: String) {
                Log.d(TAG, "onTokenSuccess: $ret")
                handleResult(ret, true)
            }

            override fun onTokenFailed(ret: String) {
                Log.e(TAG, "onTokenFailed: $ret")
                handleResult(ret, false)
            }
        }
        verifyHelper?.setAuthListener(tokenListener)
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
                 // Use SERVICE_TYPE_LOGIN constant as per demo
                 Log.d(TAG, "calling verifyHelper.checkEnvAvailable(SERVICE_TYPE_LOGIN)")
                 verifyHelper?.checkEnvAvailable(UMVerifyHelper.SERVICE_TYPE_LOGIN)
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
                     // Release listener as per demo
                     verifyHelper?.releasePreLoginResultListener()
                     promise.resolve(ret)
                }
                override fun onTokenFailed(code: String, msg: String) {
                    Log.e(TAG, "accelerateLoginPage failed: $code, $msg")
                    // Release listener as per demo
                    verifyHelper?.releasePreLoginResultListener()
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

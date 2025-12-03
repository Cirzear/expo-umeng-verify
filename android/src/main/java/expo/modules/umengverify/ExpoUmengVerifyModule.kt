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
              
              if (UMResultCode.CODE_ERROR_USER_CHECKBOX == tokenRet.code ||
                  UMResultCode.CODE_ERROR_USER_PROTOCOL_CONTROL == tokenRet.code ||
                  UMResultCode.CODE_ERROR_USER_LOGIN_BTN == tokenRet.code) {
                  Log.d(TAG, "Ignoring UI event in handleResult: ${tokenRet.code}")
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

  private fun parseColor(colorString: String?): Int {
      if (colorString.isNullOrEmpty()) return Color.TRANSPARENT
      return try {
          Color.parseColor(colorString)
      } catch (e: Exception) {
          Log.w(TAG, "Invalid color: $colorString", e)
          Color.TRANSPARENT
      }
  }

  private fun configAuthPage(context: Context, config: Map<String, Any>?) {
      // Set up UI event listener first
      verifyHelper?.setUIClickListener { code, _, jsonString ->
          try {
              val jsonObj = JSONObject(jsonString)
              when (code) {
                  UMResultCode.CODE_ERROR_USER_CANCEL -> {
                      Log.d(TAG, "User cancelled login (UI)")
                      sendEvent("onUMVerifyEvent", mapOf(
                          "type" to "onUserCancel"
                      ))
                      verifyHelper?.quitLoginPage()
                  }
                  UMResultCode.CODE_ERROR_USER_SWITCH -> {
                      Log.d(TAG, "User switched to other login method")
                      sendEvent("onUMVerifyEvent", mapOf(
                          "type" to "onSwitchAccount"
                      ))
                  }
                  UMResultCode.CODE_ERROR_USER_LOGIN_BTN -> {
                      Log.d(TAG, "User clicked login button")
                      val isChecked = jsonObj.optBoolean("isChecked", false)
                      sendEvent("onUMVerifyEvent", mapOf(
                          "type" to "onLoginButtonClick",
                          "data" to mapOf("isChecked" to isChecked)
                      ))
                  }
                  UMResultCode.CODE_ERROR_USER_CHECKBOX -> {
                      val isChecked = jsonObj.optBoolean("isChecked", false)
                      Log.d(TAG, "Checkbox state changed: $isChecked")
                      sendEvent("onUMVerifyEvent", mapOf(
                          "type" to "onCheckboxChange",
                          "data" to mapOf("isChecked" to isChecked)
                      ))
                  }
                  UMResultCode.CODE_ERROR_USER_PROTOCOL_CONTROL -> {
                      val name = jsonObj.optString("name", "")
                      val url = jsonObj.optString("url", "")
                      Log.d(TAG, "Protocol clicked: $name, $url")
                      sendEvent("onUMVerifyEvent", mapOf(
                          "type" to "onProtocolClick",
                          "data" to mapOf("name" to name, "url" to url)
                      ))
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

      // Determine screen orientation
      var authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      if (Build.VERSION.SDK_INT == 26) {
          authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
      }

      // Extract UI configuration
      val uiConfig = config?.get("ui") as? Map<String, Any>
      val privacyConfig = uiConfig?.get("privacy") as? Map<String, Any>
      val logoConfig = uiConfig?.get("logo") as? Map<String, Any>
      val phoneNumberConfig = uiConfig?.get("phoneNumber") as? Map<String, Any>
      val sloganConfig = uiConfig?.get("slogan") as? Map<String, Any>
      val loginButtonConfig = uiConfig?.get("loginButton") as? Map<String, Any>
      val switchButtonConfig = uiConfig?.get("switchButton") as? Map<String, Any>
      val checkboxConfig = uiConfig?.get("checkbox") as? Map<String, Any>
      val navigationBarConfig = uiConfig?.get("navigationBar") as? Map<String, Any>
      val statusBarConfig = uiConfig?.get("statusBar") as? Map<String, Any>
      val dialogConfig = uiConfig?.get("dialog") as? Map<String, Any>
      val pageConfig = uiConfig?.get("page") as? Map<String, Any>

      val builder = UMAuthUIConfig.Builder()

      // === Privacy Configuration ===
      val privacyOne = privacyConfig?.get("privacyOne") as? Map<String, Any>
      val privacyTwo = privacyConfig?.get("privacyTwo") as? Map<String, Any>
      val privacyThree = privacyConfig?.get("privacyThree") as? Map<String, Any>
      
      if (privacyOne != null) {
          val name = privacyOne["name"] as? String
          val url = privacyOne["url"] as? String
          if (!name.isNullOrEmpty() && !url.isNullOrEmpty()) {
              builder.setAppPrivacyOne(name, url)
          }
      }
      if (privacyTwo != null) {
          val name = privacyTwo["name"] as? String
          val url = privacyTwo["url"] as? String
          if (!name.isNullOrEmpty() && !url.isNullOrEmpty()) {
              builder.setAppPrivacyTwo(name, url)
          }
      }
      if (privacyThree != null) {
          val name = privacyThree["name"] as? String
          val url = privacyThree["url"] as? String
          if (!name.isNullOrEmpty() && !url.isNullOrEmpty()) {
              builder.setAppPrivacyThree(name, url)
          }
      }

      // Privacy colors
      val privacyColor = privacyConfig?.get("privacyColor") as? Map<String, Any>
      if (privacyColor != null) {
          val normalColor = parseColor(privacyColor["normal"] as? String)
          val clickableColor = parseColor(privacyColor["clickable"] as? String)
          builder.setAppPrivacyColor(normalColor, clickableColor)
      }

      // Privacy connect texts
      val privacyConnectTexts = privacyConfig?.get("privacyConnectTexts") as? List<*>
      if (privacyConnectTexts != null && privacyConnectTexts.size == 3) {
          builder.setPrivacyConectTexts(arrayOf(
              privacyConnectTexts[0] as? String ?: "",
              privacyConnectTexts[1] as? String ?: "",
              privacyConnectTexts[2] as? String ?: ""
          ))
      }

      privacyConfig?.get("privacyOperatorIndex")?.let {
          builder.setPrivacyOperatorIndex((it as? Number)?.toInt() ?: 0)
      }

      privacyConfig?.get("privacyState")?.let {
          builder.setPrivacyState(it as? Boolean ?: false)
      }

      privacyConfig?.get("vendorPrivacyPrefix")?.let {
          builder.setVendorPrivacyPrefix(it as? String ?: "")
      }

      privacyConfig?.get("vendorPrivacySuffix")?.let {
          builder.setVendorPrivacySuffix(it as? String ?: "")
      }

      // === Logo Configuration ===
      logoConfig?.get("imagePath")?.let {
          builder.setLogoImgPath(it as? String)
      }
      logoConfig?.get("width")?.let {
          builder.setLogoWidth((it as? Number)?.toInt() ?: 0)
      }
      logoConfig?.get("height")?.let {
          builder.setLogoHeight((it as? Number)?.toInt() ?: 0)
      }
      logoConfig?.get("offsetY")?.let {
          builder.setLogoOffsetY((it as? Number)?.toInt() ?: 0)
      }
      logoConfig?.get("hidden")?.let {
          if (it as? Boolean == true) {
              builder.setLogoHidden(true)
          }
      }

      // === Phone Number Configuration ===
      phoneNumberConfig?.get("textSize")?.let {
          builder.setNumberSizeDp((it as? Number)?.toInt() ?: 17)
      }
      phoneNumberConfig?.get("textColor")?.let {
          builder.setNumberColor(parseColor(it as? String))
      }
      phoneNumberConfig?.get("offsetY")?.let {
          builder.setNumFieldOffsetY((it as? Number)?.toInt() ?: 0)
      }

      // === Slogan Configuration ===
      sloganConfig?.get("text")?.let {
          builder.setSloganText(it as? String)
      }
      sloganConfig?.get("textSize")?.let {
          builder.setSloganTextSizeDp((it as? Number)?.toInt() ?: 11)
      }
      sloganConfig?.get("textColor")?.let {
          builder.setSloganTextColor(parseColor(it as? String))
      }
      sloganConfig?.get("offsetY")?.let {
          builder.setSloganOffsetY((it as? Number)?.toInt() ?: 0)
      }
      sloganConfig?.get("hidden")?.let {
          if (it as? Boolean == true) {
              builder.setSloganHidden(true)
          }
      }

      // === Login Button Configuration ===
      loginButtonConfig?.get("text")?.let {
          builder.setLogBtnText(it as? String)
      }
      loginButtonConfig?.get("textSize")?.let {
          builder.setLogBtnTextSizeDp((it as? Number)?.toInt() ?: 16)
      }
      loginButtonConfig?.get("textColor")?.let {
          builder.setLogBtnTextColor(parseColor(it as? String))
      }
      loginButtonConfig?.get("width")?.let {
          builder.setLogBtnWidth((it as? Number)?.toInt() ?: 0)
      }
      loginButtonConfig?.get("height")?.let {
          builder.setLogBtnHeight((it as? Number)?.toInt() ?: 0)
      }
      loginButtonConfig?.get("offsetY")?.let {
          builder.setLogBtnOffsetY((it as? Number)?.toInt() ?: 0)
      }
      loginButtonConfig?.get("marginLeftAndRight")?.let {
          builder.setLogBtnMarginLeftAndRight((it as? Number)?.toInt() ?: 0)
      }
      loginButtonConfig?.get("backgroundPath")?.let {
          builder.setLogBtnBackgroundPath(it as? String)
      }

      // === Switch Button Configuration ===
      switchButtonConfig?.get("text")?.let {
          builder.setSwitchAccText(it as? String)
      }
      switchButtonConfig?.get("textSize")?.let {
          builder.setSwitchAccTextSize((it as? Number)?.toInt() ?: 14)
      }
      switchButtonConfig?.get("textColor")?.let {
          builder.setSwitchAccTextColor(parseColor(it as? String))
      }
      switchButtonConfig?.get("hidden")?.let {
          if (it as? Boolean == true) {
              builder.setSwitchAccHidden(true)
          } else {
              builder.setSwitchAccHidden(false)
          }
      } ?: run {
          // Default to hidden as per demo
          builder.setSwitchAccHidden(true)
      }
      switchButtonConfig?.get("offsetY")?.let {
          builder.setSwitchOffsetY((it as? Number)?.toInt() ?: 0)
      }

      // === Checkbox Configuration ===
      checkboxConfig?.get("hidden")?.let {
          builder.setCheckboxHidden(it as? Boolean ?: false)
      }
      checkboxConfig?.get("defaultState")?.let {
          builder.setPrivacyState(it as? Boolean ?: false)
      }

      // === Navigation Bar Configuration ===
      navigationBarConfig?.get("title")?.let {
          builder.setNavText(it as? String)
      }
      navigationBarConfig?.get("titleColor")?.let {
          builder.setNavTextColor(parseColor(it as? String))
      }
      navigationBarConfig?.get("backgroundColor")?.let {
          builder.setNavColor(parseColor(it as? String))
      }
      navigationBarConfig?.get("hidden")?.let {
          if (it as? Boolean == true) {
              builder.setNavHidden(true)
          }
      }
      navigationBarConfig?.get("returnButtonHidden")?.let {
          if (it as? Boolean == true) {
              builder.setNavReturnHidden(true)
          }
      }

      // === Status Bar Configuration ===
      statusBarConfig?.get("color")?.let {
          builder.setStatusBarColor(parseColor(it as? String))
      } ?: run {
          // Default to transparent for immersive experience
          builder.setStatusBarColor(Color.TRANSPARENT)
      }
      
      statusBarConfig?.get("uiFlag")?.let {
          builder.setStatusBarUIFlag((it as? Number)?.toInt() ?: View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
      } ?: run {
          builder.setStatusBarUIFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
      }
      
      statusBarConfig?.get("lightColor")?.let {
          builder.setLightColor(it as? Boolean ?: true)
      } ?: run {
          builder.setLightColor(true)
      }

      // === Dialog Configuration ===
      val mode = uiConfig?.get("mode") as? String
      if (mode != null && mode.startsWith("dialog")) {
          dialogConfig?.get("width")?.let {
              builder.setDialogWidth((it as? Number)?.toInt() ?: 0)
          }
          dialogConfig?.get("height")?.let {
              builder.setDialogHeight((it as? Number)?.toInt() ?: 0)
          }
          dialogConfig?.get("bottom")?.let {
              builder.setDialogBottom(it as? Boolean ?: false)
          }
          dialogConfig?.get("tapMaskToClose")?.let {
              builder.setTapAuthPageMaskClosePage(it as? Boolean ?: false)
          }
      }

      // === Page Configuration ===
      pageConfig?.get("backgroundColor")?.let {
          builder.setPageBackgroundPath(it as? String)
      }
      pageConfig?.get("backgroundImagePath")?.let {
          builder.setPageBackgroundPath(it as? String)
      }
      pageConfig?.get("enterAnimation")?.let {
          val exitAnim = pageConfig["exitAnimation"] as? String ?: "out_activity"
          builder.setAuthPageActIn(it as? String, exitAnim)
      }
      pageConfig?.get("exitAnimation")?.let {
          val enterAnim = pageConfig["enterAnimation"] as? String ?: "in_activity"
          builder.setAuthPageActOut(enterAnim, it as? String)
      }

      // Page orientation
      when (pageConfig?.get("orientation") as? String) {
          "portrait" -> authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
          "landscape" -> authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          "sensor-portrait" -> authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
          "sensor-landscape" -> authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }

      // === Other Configuration ===
      uiConfig?.get("hideLoginToast")?.let {
          if (it as? Boolean == true) {
              builder.setLogBtnToastHidden(true)
          }
      } ?: run {
          // Default to hiding toast
          builder.setLogBtnToastHidden(true)
      }

      builder.setWebNavTextSizeDp(20)
      builder.setScreenOrientation(authPageOrientation)

      verifyHelper?.setAuthUIConfig(builder.create())
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoUmengVerify")

    // Define events that can be sent to JavaScript
    Events("onUMVerifyEvent")


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

package `in`.breeze.blaze

import android.app.Activity
import android.content.Intent
import androidx.annotation.Keep
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.UUID


typealias CallbackFn = (event: JSONObject) -> Unit;

class Blaze {

  private var webView: BlazeWebView? = null;
  private var contextRef: WeakReference<Activity>? = null;
  private var callbackFn: CallbackFn? = null;
  private var isInitialized: Boolean = false;
  private var configuration: BlazeConfiguration? = null

  @Synchronized
  fun initiate(context: Activity, configuration: BlazeConfiguration, callbackFn: CallbackFn) {
    val validated = configuration.validated()
    if (isInitialized) {
      if (validated == this.configuration) {
        return
      }
      throw BlazeConfigurationException(
        "configuration",
        "Blaze is already initiated with a different configuration; call terminate() first"
      )
    }
    this.contextRef = WeakReference(context)
    this.callbackFn = callbackFn
    this.configuration = validated
    val initiatePayload = validated.toInitiatePayload(UUID.randomUUID().toString())
    context.runOnUiThread {
      this.webView = BlazeWebView(context, initiatePayload, callbackFn, validated.frameUrl())
    }
    this.isInitialized = true
  }

  fun initiate(context: Activity, initiatePayload: JSONObject, callbackFn: CallbackFn) {
    this.contextRef = WeakReference(context)
    this.callbackFn = callbackFn;
    if (!isInitialized) {
      context.runOnUiThread {
        this.webView = BlazeWebView(context, initiatePayload, callbackFn)
      }
      this.isInitialized = true
    }
  }

  fun process(payload: JSONObject) {
    this.webView?.process(payload)
  }

  fun handleBackPress(): Boolean {
    return this.webView?.handleBackPress() ?: true

  }

  @Synchronized
  fun terminate() {
    this.webView?.terminate()
    webView = null
    configuration = null
    callbackFn = null
    contextRef?.clear()
    contextRef = null
    isInitialized = false
  }

  @Keep
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
  }

  @Keep
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
  }

}
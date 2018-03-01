package com.simpleel.cordova.crosspay.wxpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

import com.simpleel.cordova.crosspay.CrossPay;

public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

  private static final String LOG_TAG = WXPayEntryActivity.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CrossPay.wxAPI.handleIntent(getIntent(), this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    CrossPay.wxAPI.handleIntent(intent, this);
  }

  @Override
  public void onReq(BaseReq req) {
    finish();
  }

  @Override
  public void onResp(BaseResp resp) {
    Log.d(LOG_TAG, "onPayFinish, errCode = " + resp.errCode);

    switch (resp.errCode) {
    case BaseResp.ErrCode.ERR_OK:
      switch (resp.getType()) {
      case ConstantsAPI.COMMAND_SENDAUTH:
        auth(resp);
        break;
      case ConstantsAPI.COMMAND_PAY_BY_WX:
      default:
        CrossPay.currentCallbackContext.success();
        break;
      }
      break;
    // case BaseResp.ErrCode.ERR_USER_CANCEL:
    //   CrossPay.currentCallbackContext.error("The oder has been cancelled.");
    //   break;
    // case BaseResp.ErrCode.ERR_AUTH_DENIED:
    //   CrossPay.currentCallbackContext.error("Authorization failure.");
    //   break;
    // case BaseResp.ErrCode.ERR_SENT_FAILED:
    //   CrossPay.currentCallbackContext.error("Send failure.");
    //   break;
    // case BaseResp.ErrCode.ERR_UNSUPPORT:
    //   CrossPay.currentCallbackContext.error("Wechat not suport.");
    //   break;
    // case BaseResp.ErrCode.ERR_COMM:
    //   CrossPay.currentCallbackContext.error("Common error.");
    //   break;
    default:
      JSONObject response = new JSONObject();
      try {
        response.put("errCode", resp.errCode);
        response.put("errStr", resp.errStr);
        response.put("transaction", resp.transaction);
        response.put("openId", resp.openId);
        CrossPay.currentCallbackContext.error(response.toString());
      } catch (JSONException e) {
        Log.e(CrossPay.TAG + ".onResp", e.getMessage());
        CrossPay.currentCallbackContext.error("Unkown error");
      }
      break;
    }

    finish();
  }

  protected void auth(BaseResp resp) {
    SendAuth.Resp res = ((SendAuth.Resp) resp);

    Log.d(CrossPay.TAG, res.toString());

    // get current callback context
    CallbackContext ctx = CrossPay.currentCallbackContext;

    if (ctx == null) {
      return;
    }

    JSONObject response = new JSONObject();
    try {
      response.put("code", res.code);
      response.put("state", res.state);
      response.put("country", res.country);
      response.put("lang", res.lang);
    } catch (JSONException e) {
      Log.e(CrossPay.TAG, e.getMessage());
    }

    ctx.success(response);
  }
}
package com.simpleel.cordova;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;
import android.util.Log;
import android.annotation.SuppressLint;

// alipay sdk
import com.alipay.sdk.app.PayTask;

// wechat sdk
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

/**
 * This class echoes a string called from JavaScript.
 */
public class CrossPay extends CordovaPlugin {
    public static final String TAG = "cordova.plugins.CrossPay";
    public static IWXAPI wxAPI;
    public static CallbackContext currentCallbackContext;

    private static final int SDK_PAY_FLAG = 1;

    protected void aliPayment(String orderInfo, final CallbackContext callbackContext) {
        final String payInfo = orderInfo;

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PayTask alipay = new PayTask(cordova.getActivity());
                Map<String, String> result = alipay.payV2(payInfo, true);

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);

                PayResult payResult = new PayResult(result);
                String resultInfo = payResult.getResult(); //同步返回需要验证的信息
                String resultStatus = payResult.getResultStatus();
                // resultStatus 为 9000 时代表支付成功
                if (TextUtils.equals(resultStatus, "9000")) {
                    // 该笔订单是否真实支付成功，需要依赖服务端的异步通知
                    callbackContext.success(new JSONObject(result));
                } else {
                    // 该笔订单真实的支付结果，需要依赖服务端的异步通知
                    callbackContext.error(new JSONObject(result));
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @SuppressWarnings("unused")
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SDK_PAY_FLAG: {
                @SuppressWarnings("unchecked")
                PayResult payResult = new PayResult((Map<String, String>) msg.obj);
                /**
                 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                 */
                String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                String resultStatus = payResult.getResultStatus();
                // 判断resultStatus 为9000则代表支付成功
                // 判断resultStatus 为9000则代表支付成功
                if (TextUtils.equals(resultStatus, "9000")) {
                    // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                    Toast.makeText(cordova.getActivity(), "支付成功" + resultStatus, Toast.LENGTH_SHORT);
                } else {
                    // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                    Toast.makeText(cordova.getActivity(), "支付失败" + resultStatus, Toast.LENGTH_SHORT);
                }
                break;
            }
            default:
                break;
            }
        }
    };

    protected boolean wechatPayment(CordovaArgs args, CallbackContext callbackContext) {
        final JSONObject params;
        try {
            params = args.getJSONObject(0);
        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
            return true;
        }

        PayReq req = new PayReq();

        try {
            req.appId = params.getString("appId");
            req.partnerId = params.getString("partnerId");
            req.prepayId = params.getString("prepayId");
            req.nonceStr = params.getString("nonceStr");
            req.timeStamp = params.getString("timeStamp");
            req.sign = params.getString("sign");
            req.packageValue = params.getString("package");

            wxAPI = WXAPIFactory.createWXAPI(webView.getContext(), req.appId, true);
            wxAPI.registerApp(req.appId);
            if (wxAPI.sendReq(req)) {
                // send no result
                sendNoResultPluginResult(callbackContext);
            } else {
                // send error
                callbackContext.error("WechatPayment request has been sent unsuccessfully.");
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }

        return true;
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        final JSONObject params;
        if (action.equals("wechatPayment")) {
            wechatPayment(args, callbackContext);
            return true;
        } else if (action.equals("aliPayment")) {
            params = args.getJSONObject(0);
            String orderInfo = params.getString("order");
            aliPayment(orderInfo, callbackContext);
            return true;
        }
        return false;
    }

    private void sendNoResultPluginResult(CallbackContext callbackContext) {
        currentCallbackContext = callbackContext;

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }
}

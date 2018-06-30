# cordova-plugin-crosspay
cordova跨平台支付插件，现支持ios、android的微信支付及支付宝支付。</br>
支持 cordova 7，在 cordova 6 环境下请安装 0.0.2 版本。

# 安装方法
```
cordova plugin add https://github.com/toventang/cordova-plugin-crosspay --save</br>
或</br>
cordova plugin add cordova-plugin-crosspay
```
支付所需的APPID等都由后台接口返回，不放在APP代码里。

# 使用方法
``` typescript
declare var cordova:any;

export class HomePage {
  constructor(){}

  aliPay(){
    // 参数参照支付宝官方开发文档
    cordova.plugins.crosspay.aliPay(
        {
          appId: "后台接口返回的APPID",
          order: "后台返回的orderStr"
        },
        function() {
          // 支付成功
        },
        function(error) {
          // 支付失败
          console.log('错误描述', error);
        }
      );
  }
  
  wechatPay(){
    // 参数参照微信支付官方开发文档
    cordova.plugins.crosspay.wechatPay(
        {
          appId: "后台接口返回的APPID",
          partnerId: "商户ID",
          prepayId: "预付ID",
          nonceStr: "noncestr"
          timeStamp: "timeStamp",
          sign: "sign"
          package: "Sign=WXPay"
        },
        function() {
          // 支付成功
        },
        function(error) {
          // 支付失败
          console.log('错误描述', error);
        }
      );
  }
}
```

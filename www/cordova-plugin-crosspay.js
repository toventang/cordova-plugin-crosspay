var exec = require('cordova/exec');

var crosspay = {
    wechatPay: function(params, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CrossPay', 'wechatPayment', [params]);
    },

    aliPay: function(params, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'CrossPay', 'aliPayment', [params]);
    }
}

module.exports = crosspay;
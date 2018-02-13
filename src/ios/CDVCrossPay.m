/********* Cordova Plugin Implementation *******/

#import "CDVCrossPay.h"

@implementation CDVCrossPay

#pragma mark "API"

- (void)aliPayment:(CDVInvokedUrlCommand *)command {
    self.currentCallbackId = command.callbackId;
    self.payType = @"alipay";
    NSDictionary *params = [command.arguments objectAtIndex:0];
    self.appId = [params objectForKey:@"appId"];
    NSString *orderString = [params objectForKey:@"order"];
    NSMutableString *appScheme = [NSMutableString string];
    [appScheme appendFormat:@"ALI%@", self.appId];
    [[AlipaySDK defaultService] payOrder:orderString fromScheme:appScheme callback:^(NSDictionary *resultDic) {
        CDVPluginResult *pluginResult;
        
        if ([[resultDic objectForKey:@"resultStatus"]  isEqual: @"9000"]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDic];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.currentCallbackId];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:resultDic];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.currentCallbackId];
        }
        
    }];
}

- (void)wechatPayment:(CDVInvokedUrlCommand *)command {
    self.payType = @"wechat";
    NSDictionary *params = [command.arguments objectAtIndex:0];
    if (!params) {
        [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
        return ;
    }

    self.appId = [params objectForKey:@"appId"];

    PayReq* req  = [[PayReq alloc] init];
    req.partnerId = [params objectForKey:@"partnerId"];
    req.prepayId = [params objectForKey:@"prepayId"];
    req.nonceStr = [params objectForKey:@"nonceStr"];
    NSMutableString *stamp  = [params objectForKey:@"timeStamp"];
    req.timeStamp = stamp.intValue;
    req.package = [params objectForKey:@"package"];
    req.sign = [params objectForKey:@"sign"];

    // regist wechat appid
    [WXApi registerApp:self.appId];

    if ([WXApi sendReq:req]) {
        self.currentCallbackId = command.callbackId;
    } else {
        [self failWithCallbackID:command.callbackId withMessage:@"发送失败"];
    }

}

#pragma mark "WXApiDelegate"

- (void)onReq:(BaseReq *)req {
    NSLog(@"%@", req);
}

- (void)onResp:(BaseResp *)resp {
    BOOL success = NO;
    NSString *message = @"Unkonwn";

    switch (resp.errCode) {
        case WXSuccess:
            success = YES;
            break;
        case WXErrCodeCommon:
            message = @"普通错误";
            break;
        case WXErrCodeUserCancel:
            message = @"用户点击取消并返回";
            break;
        case WXErrCodeSentFail:
            message = @"发送失败";
            break;
        case WXErrCodeAuthDeny:
            message = @"授权失败";
            break;
        case WXErrCodeUnsupport:
            message = @"微信不支持";
            break;
        default:
            message = @"未知错误";
    }

    if (success) {
        if ([resp isKindOfClass:[SendAuthResp class]]) {
             NSString *strMsg = [NSString stringWithFormat:@"支付结果：retcode = %d, retstr = %@", resp.errCode,resp.errStr];
            
            CDVPluginResult *commandResult = nil;
            
            if (resp.errCode == 0)
            {
                commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:strMsg];
            }
            else
            {
                commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:strMsg];
            }
            
            [self.commandDelegate sendPluginResult:commandResult callbackId:self.currentCallbackId];
        } else {
            NSLog(@"回调不是支付类型");
            [self successWithCallbackID:self.currentCallbackId];
        }
    } else {
        [self failWithCallbackID:self.currentCallbackId withMessage:message];
    }
}

#pragma mark "CDVPlugin Overrides"

- (void)handleOpenURL:(NSNotification *)notification {
    NSURL *url =[notification object];
    NSMutableString *schemeStr = [NSMutableString string];

    if ([self.payType isEqualToString:@"alipay"]) {
        [schemeStr appendFormat:@"ALI%@", self.appId];
    } else {
        [schemeStr appendFormat:@"WECHAT%@", self.appId];
    }
    if ([url isKindOfClass:[NSURL class]] && [url.scheme isEqualToString:schemeStr]) {
        if ([self.payType isEqualToString: @"wechat"]) {
            [WXApi handleOpenURL:url delegate:self];
        } else {
            [[AlipaySDK defaultService] processOrderWithPaymentResult:url standbyCallback:^(NSDictionary *resultDic) {
            
                CDVPluginResult *pluginResult;
                
                if ([[resultDic objectForKey:@"resultStatus"]  isEqual: @"9000"]) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDic];
                    [self.commandDelegate sendPluginResult: pluginResult callbackId: self.currentCallbackId];
                } else {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:resultDic];
                    [self.commandDelegate sendPluginResult: pluginResult callbackId: self.currentCallbackId];
                }
            }];
        }
    }
}

#pragma mark "Private methods"

- (void)successWithCallbackID:(NSString *)callbackID {
    [self successWithCallbackID:callbackID withMessage:@"OK"];
}

- (void)successWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message {
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

- (void)failWithCallbackID:(NSString *)callbackID withError:(NSError *)error {
    [self failWithCallbackID:callbackID withMessage:[error localizedDescription]];
}

- (void)failWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message {
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

@end

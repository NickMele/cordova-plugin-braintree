#import "BraintreePlugin.h"

#import <Cordova/CDVAvailability.h>

@implementation BraintreePlugin

- (void)pluginInitialize {
  NSLog(@"Starting Braintree Cordova Plugin");

  [BTAppSwitch setReturnURLScheme:[self returnUrl]];

  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleOpenURLWithApplicationSourceAndAnnotation:) name:CDVPluginHandleOpenURLWithAppSourceAndAnnotationNotification object:nil];
}

- (void)handleOpenURLWithApplicationSourceAndAnnotation:(NSNotification*)notification {
  NSDictionary*  notificationData = [notification object];

  if ([notificationData isKindOfClass: NSDictionary.class]) {
    NSURL* url = notificationData[@"url"];
    NSString* sourceApplication = notificationData[@"sourceApplication"];
    id annotation = notificationData[@"annotation"];

    if ([url.scheme localizedCaseInsensitiveCompare:[self returnUrl]] == NSOrderedSame) {
      [BTAppSwitch handleOpenURL:url sourceApplication:sourceApplication];
    }
  }
}

- (NSString *)returnUrl {
  NSString *bundleID = [[NSBundle mainBundle] bundleIdentifier];
  NSString *bundleSuffix = @".payments";

  return [bundleID stringByAppendingString:bundleSuffix];
}

- (void)initialize:(CDVInvokedUrlCommand*)command {
  NSString *clientToken = command.arguments[0];

  self.apiClient = [[BTAPIClient alloc] initWithAuthorization:clientToken];
  self.venmoDriver = [[BTVenmoDriver alloc] initWithAPIClient:self.apiClient];
}

- (void)isVenmoAvailable:(CDVInvokedUrlCommand*)command {
  if (self.venmoDriver) {
    if ([self.venmoDriver isiOSAppAvailableForAppSwitch]) {
      CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
      CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"This device cannot make venmo payments"];
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
  } else {
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"This device cannot make venmo payments"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

- (void)makeVenmoPayment:(CDVInvokedUrlCommand*)command {
  if (self.venmoDriver) {
    [self.venmoDriver authorizeAccountAndVault:NO completion:^(BTVenmoAccountNonce * _Nullable venmoAccount, NSError * _Nullable error) {
      if (venmoAccount) {
        // You got a Venmo nonce!
        NSLog(@"%@", venmoAccount);

        NSMutableDictionary *response = [[NSMutableDictionary alloc] init];

        [response setObject:venmoAccount.nonce forKey:@"nonce"];
        [response setObject:venmoAccount.description forKey:@"description"];
        [response setObject:venmoAccount.username forKey:@"username"];

        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:response];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      } else if (error) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to complete venmo payment"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      }
    }];
  } else {
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString: @"This device cannot make venmo payments"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

@end

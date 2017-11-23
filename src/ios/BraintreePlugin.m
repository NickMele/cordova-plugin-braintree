#import "BraintreePlugin.h"
#import <Cordova/CDVAvailability.h>

static short int *const ERROR_PLUGIN_NOT_INITIALIZED = 1;
static short int *const ERROR_INITIALIZATION_ERROR = 2;
static short int *const ERROR_VENMO_NOT_AVAILABLE = 3;
static short int *const ERROR_AUTHORIZATION_ERROR = 4;
static short int *const ERROR_USER_CANCELLED_AUTHORIZATION = 5;
static short int *const ERROR_VENMO_NOT_ENABLED_FOR_MERCHANT = 6;

@implementation BraintreePlugin

- (void)pluginInitialize {
  NSLog(@"Starting Braintree Cordova Plugin");

  NSLog(@"Setting return url %@", [self returnUrl]);
  [BTAppSwitch setReturnURLScheme:[self returnUrl]];

  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleOpenURLWithApplicationSourceAndAnnotation:) name:CDVPluginHandleOpenURLWithAppSourceAndAnnotationNotification object:nil];
}

- (void)handleOpenURLWithApplicationSourceAndAnnotation:(NSNotification*)notification {
  NSDictionary*  notificationData = [notification object];

  if ([notificationData isKindOfClass: NSDictionary.class]) {
    NSURL* url = notificationData[@"url"];
    NSString* sourceApplication = notificationData[@"sourceApplication"];

    if ([url.scheme localizedCaseInsensitiveCompare:[self returnUrl]] == NSOrderedSame) {
      NSLog(@"Handling url: %@", url);
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

  NSLog(@"Setting apiClient and venmoDriver");

  self.apiClient = [[BTAPIClient alloc] initWithAuthorization:clientToken];

  if (self.apiClient) {
    self.venmoDriver = [[BTVenmoDriver alloc] initWithAPIClient:self.apiClient];
  } else {
    NSLog(@"Failed to initialize the API Client");
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self getErrorMessage:ERROR_INITIALIZATION_ERROR message:@"Failed to initialize the API Client"]];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

- (void)isVenmoAvailable:(CDVInvokedUrlCommand*)command {
  if (self.venmoDriver) {
    if ([self.venmoDriver isiOSAppAvailableForAppSwitch]) {
      NSLog(@"Venmo is available on this device");
      CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
      NSLog(@"Venmo is not available on this device");
      CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:FALSE];
      [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
  } else {
    NSLog(@"The venmoDriver could not be found");
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self getErrorMessage:ERROR_PLUGIN_NOT_INITIALIZED]];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

- (void)authorizeVenmoAccount:(CDVInvokedUrlCommand*)command {
  if (self.venmoDriver) {
    NSLog(@"Authorizing venmo account");
    [self.venmoDriver authorizeAccountAndVault:NO completion:^(BTVenmoAccountNonce * _Nullable venmoAccount, NSError * _Nullable error) {
      if (venmoAccount) {
        // You got a Venmo nonce!
        NSLog(@"Venmo account authorized: %@", venmoAccount);

        NSMutableDictionary *response = [[NSMutableDictionary alloc] init];

        [response setObject:venmoAccount.nonce forKey:@"nonce"];
        [response setObject:venmoAccount.username forKey:@"username"];

        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:response];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      } else if (error) {
        NSLog(@"Error authorizing account: %@", error);
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self getErrorMessage:ERROR_AUTHORIZATION_ERROR message:error.localizedDescription]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      } else {
        NSLog(@"User cancelled venmo request");
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self getErrorMessage:ERROR_USER_CANCELLED_AUTHORIZATION]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
      }
    }];
  } else {
    NSLog(@"The venmoDriver could not be found");
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self getErrorMessage:ERROR_PLUGIN_NOT_INITIALIZED]];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
  }
}

- (NSMutableDictionary *)getErrorMessage:(short int *)errorCode {
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

  [result setObject:errorCode forKey:@"errorCode"];

  return result;
}

- (NSMutableDictionary *)getErrorMessage:(short int *)errorCode message:(NSString *)message {
  NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

  [result setObject:errorCode forKey:@"errorCode"];
  [result setObject:message forKey:@"message"];

  return result;
}

@end

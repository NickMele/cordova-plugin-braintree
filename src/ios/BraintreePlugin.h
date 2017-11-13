#import <Cordova/CDVPlugin.h>
#import "BraintreeCore.h"
#import "BraintreeVenmo.h"

@interface BraintreePlugin : CDVPlugin

@property (nonatomic, strong) BTVenmoDriver *venmoDriver;
@property (nonatomic, strong) BTAPIClient *apiClient;

- (NSString *)returnUrl;

- (void)initialize:(CDVInvokedUrlCommand*)command;
- (void)isVenmoAvailable:(CDVInvokedUrlCommand*)command;

@end

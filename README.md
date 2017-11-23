# Cordova Plugin Braintree

Bridges the native Braintree SDK for Cordova

## Installation
```shell
cordova plugin add https://github.com/NickMele/cordova-plugin-braintree
```

## Usage

### `initialize(clientToken, successFn, failureFn)`

Initializes the plugin with a client token that you receive from your server.

https://developers.braintreepayments.com/guides/authorization/client-token

#### `@param {String} clientToken`  The client token from your server
#### `@param {Function} successFn`  Initialization successful callback
#### `@param {Function} failureFn`  Failed to initialize callback
#### `failureFn` Parameters
##### `@param {Object} error`  Failed to initialize callback
###### `@param {Integer} errorCode` Error code
###### `@param {String} message` Additional message from the native plugin

```javascript
BraintreePlugin.initialize("abcd123", successFn, failureFn);
```

---

### `isVenmoAvailable(successFn, failureFn)`

Checks if the Venmo app is available on the device

#### `successFn` Parameters
##### `@param {Boolean} isAvailable` Boolean indicating if Venmo is available on the device
#### `failureFn` Parameters
##### `@param {Object} error`  Failed to initialize callback
###### `@param {Integer} errorCode` Error code
###### `@param {String} message` Additional message from the native plugin

```javascript
BraintreePlugin.isVenmoAvailable(function(isAvailable) {
  if (isAvailable) {
    // Show venmo button
  }
}, function(error) {
  // Assume venmo is not available
});
```

---

### `authorizeVenmoAccount(successFn, failureFn)`

Will attempt to open the Venmo app on the users device. Once the user authorizes, a payment nonce object will be returned.

#### `successFn` Parameters
##### `@param {Object} paymentNonce` Object containing the payment data to be used in Braintree
###### `@param {String} nonce` Payment nonce that can be used in Braintree
###### `@param {String} username` The username for the user in Venmo
#### `failureFn` Parameters
##### `@param {Object} error`  Failed to initialize callback
###### `@param {Integer} errorCode` Error code
###### `@param {String} message` Additional message from the native plugin

```javascript
BraintreePlugin.authorizeVenmoAccount(function(paymentNonce) {
  // paymentNonce will contain the nonce and username
}, function(error) {});
```

---

## Error Codes
| Code | Error |
|------|-------|
| 1 | Plugin is not initialized. Make sure you run `BraintreePlugin.initialize(clientToken)` |
| 2 | An error occurred during the initialization of the plugin |
| 3 | Venmo is not available on this device |
| 4 | An error occurrent during the authorization request |
| 5 | User cancelled Venmo authorization request |
| 6 | Venmo is not enabled on customers merchant account |

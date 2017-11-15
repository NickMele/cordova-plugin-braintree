var exec = require('cordova/exec');

var PLUGIN_NAME = 'BraintreePlugin';

var BraintreePlugin = {
  initialize: function(clientToken, success, failure) {
    cordova.exec(
      success,
      failure,
      PLUGIN_NAME,
      'initialize',
      [clientToken]
    );
  },
  isVenmoAvailable: function(success, failure) {
    cordova.exec(
      success,
      failure,
      PLUGIN_NAME,
      'isVenmoAvailable',
      []
    );
  },
  authorizeVenmoAccount: function(success, failure) {
    cordova.exec(
      success,
      failure,
      PLUGIN_NAME,
      'authorizeVenmoAccount',
      []
    );
  }
};

module.exports = BraintreePlugin;

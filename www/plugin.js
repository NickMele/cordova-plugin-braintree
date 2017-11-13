var exec = require('cordova/exec');

var PLUGIN_NAME = 'BraintreePlugin';

var BraintreePlugin = {
  initialize: function(clientToken) {
    cordova.exec(
      success,
      failure,
      PLUGIN_NAME,
      'initialize',
      [clientToken]
    );
  },
  isVenmoAvailable: function() {
    cordova.exec(
      success,
      failure,
      PLUGIN_NAME,
      'isVenmoAvailable',
      []
    );
  }
};

module.exports = BraintreePlugin;

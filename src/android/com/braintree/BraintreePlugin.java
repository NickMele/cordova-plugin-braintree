/**
 */
package com.braintree;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VenmoAccountNonce;

import android.util.Log;
import android.content.Context;
import android.app.Activity;

import java.util.Date;

public class BraintreePlugin extends CordovaPlugin {
  private static final String TAG = "BraintreePlugin";

  public static final int ERROR_PLUGIN_NOT_INITIALIZED = 1;
  public static final int ERROR_INITIALIZATION_ERROR = 2;
  public static final int ERROR_VENMO_NOT_AVAILABLE = 3;
  public static final int ERROR_AUTHORIZATION_ERROR = 4;
  public static final int ERROR_USER_CANCELLED_AUTHORIZATION = 5;
  public static final int ERROR_VENMO_NOT_ENABLED_FOR_MERCHANT = 6;

  public static final String ACTION_INITIALIZE = "initialize";
  public static final String ACTION_IS_VENMO_AVAILABLE = "isVenmoAvailable";
  public static final String ACTION_AUTHORIZE_VENMO_ACCOUNT = "authorizeVenmoAccount";
  public static final String ACTION_GET_DEVICE_DATA = "getDeviceData";

  private boolean isAvailable = false;
  private boolean configurationFetched = false;
  private BraintreeFragment mBraintreeFragment;
  private CallbackContext availabilityCallbackContext;
  private CallbackContext venmoAuthorizationCallbackContext;
  private ConfigurationListener configurationListener;
  private PaymentMethodNonceCreatedListener paymentMethodNonceCreatedListener;
  private BraintreeErrorListener braintreeErrorListener;
  private BraintreeCancelListener braintreeCancelListener;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    Log.d(TAG, "Starting Braintree Cordova Plugin");
  }

  @Override
  public void onDestroy() {
    removeListeners();

    super.onDestroy();
  }

  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (ACTION_INITIALIZE.equalsIgnoreCase(action)) {
      createBraintreeFragment(args.getString(0), callbackContext);
    } else if (ACTION_IS_VENMO_AVAILABLE.equalsIgnoreCase(action)) {
      isVenmoAvailable(callbackContext);
    } else if (ACTION_AUTHORIZE_VENMO_ACCOUNT.equalsIgnoreCase(action)) {
      authorizeVenmoAccount(callbackContext);
    } else if (ACTION_GET_DEVICE_DATA.equalsIgnoreCase(action)) {
      getDeviceData(callbackContext);
    }

    return true;
  }

  private void isVenmoAvailable(CallbackContext callbackContext) {
    Log.d(TAG, "Storing isVenmoAvailable callback for when configuration is fetched");

    availabilityCallbackContext = callbackContext;

    if (configurationFetched) {
      sendAvailabilityUpdate();
    }
  }

  private void authorizeVenmoAccount(CallbackContext callbackContext) {
    if (mBraintreeFragment != null) {
      Log.d(TAG, "Authorizing venmo account");

      venmoAuthorizationCallbackContext = callbackContext;

      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          Venmo.authorizeAccount(mBraintreeFragment, false);
        }
      });
    } else {
      Log.d(TAG, "No braintree fragment found");

      callbackContext.error(getError(ERROR_PLUGIN_NOT_INITIALIZED));
    }
  }

  private void getDeviceData(CallbackContext callbackContext) {
    if (mBraintreeFragment != null) {
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
              callbackContext.success(deviceData);
            }
          });
        }
      });
    } else {
      Log.d(TAG, "No braintree fragment found");

      callbackContext.error(getError(ERROR_PLUGIN_NOT_INITIALIZED));
    }
  }

  private void createBraintreeFragment(String clientToken, CallbackContext callbackContext) {
    Log.d(TAG, "Creating braintree fragment");

    try {
      Activity activity = this.cordova.getActivity();
      mBraintreeFragment = BraintreeFragment.newInstance(activity, clientToken);

      addListeners();
      callbackContext.success();
    } catch (InvalidArgumentException e) {
      Log.d(TAG, "Error creating braintree fragment: " + e.toString());

      callbackContext.error(getError(ERROR_INITIALIZATION_ERROR, e.toString()));
    }
  }

  private void addListeners() {
    Log.d(TAG, "Adding fragment listeners");

    if (configurationListener == null) {
      configurationListener = new ConfigurationListener() {
        @Override
        public void onConfigurationFetched(Configuration configuration) {
          Log.d(TAG, "Configuration fetched");
          btConfigurationFetched(configuration);
        }
      };

      mBraintreeFragment.addListener(configurationListener);
    }

    if (paymentMethodNonceCreatedListener == null) {
      paymentMethodNonceCreatedListener = new PaymentMethodNonceCreatedListener() {
        @Override
        public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
          Log.d(TAG, "Payment method nonce created");
          btPaymentMethodNonceCreated(paymentMethodNonce);
        }
      };

      mBraintreeFragment.addListener(paymentMethodNonceCreatedListener);
    }

    if (braintreeErrorListener == null) {
      braintreeErrorListener = new BraintreeErrorListener() {
        @Override
        public void onError(Exception error) {
          Log.d(TAG, "Braintree error");
          btError(error);
        }
      };

      mBraintreeFragment.addListener(braintreeErrorListener);
    }

    if (braintreeCancelListener == null) {
      braintreeCancelListener = new BraintreeCancelListener() {
        @Override
        public void onCancel(int requestCode) {
          Log.d(TAG, "Braintree activity cancelled");
          btCancel(requestCode);
        }
      };

      mBraintreeFragment.addListener(braintreeCancelListener);
    }
  }

  private void removeListeners() {
    Log.d(TAG, "Removing braintree listeners");

    if (mBraintreeFragment != null) {
      if (configurationListener != null) {
        mBraintreeFragment.removeListener(configurationListener);
      }
      if (paymentMethodNonceCreatedListener != null) {
        mBraintreeFragment.removeListener(paymentMethodNonceCreatedListener);
      }
      if (braintreeErrorListener != null) {
        mBraintreeFragment.removeListener(braintreeErrorListener);
      }
      if (braintreeCancelListener != null) {
        mBraintreeFragment.removeListener(braintreeCancelListener);
      }
    }
  }

  private void sendAvailabilityUpdate() {
    Log.d(TAG, "Sending venmo availability update to webview");

    if (availabilityCallbackContext != null) {
      availabilityCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, isAvailable));
      availabilityCallbackContext = null;
    }

    webView.postMessage("braintree.venmo.isAvailable", isAvailable);
  }

  private void btConfigurationFetched(Configuration configuration) {
    Context context = this.cordova.getActivity().getApplicationContext();

    if (configuration.getPayWithVenmo().isEnabled(context)) {
      isAvailable = true;
      Log.d(TAG, "Venmo available");
    } else if (configuration.getPayWithVenmo().isAccessTokenValid()) {
      isAvailable = false;
      Log.d(TAG, "Venmo app not installed");
    } else {
      isAvailable = false;
      Log.d(TAG, "Venmo not enabled for merchant");
    }

    if (availabilityCallbackContext != null) {
      sendAvailabilityUpdate();
    }

    configurationFetched = true;
  }

  private void btPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
    String nonce = paymentMethodNonce.getNonce();

    Log.d(TAG, "Nonce received: " + nonce);

    if (paymentMethodNonce instanceof VenmoAccountNonce) {
      Log.d(TAG, "Nonce is a VenmoAccountNonce");

      VenmoAccountNonce venmoAccountNonce = (VenmoAccountNonce) paymentMethodNonce;
      String venmoUsername = venmoAccountNonce.getUsername();

      if (venmoAuthorizationCallbackContext != null) {
        JSONObject response = new JSONObject();

        try {
          response.put("nonce", nonce);
          response.put("username", venmoUsername);
        } catch (JSONException e) {
          e.printStackTrace();
          venmoAuthorizationCallbackContext.error(getError(ERROR_AUTHORIZATION_ERROR, "There was an error processing the authorization data"));
          venmoAuthorizationCallbackContext = null;
          return;
        }

        Log.d(TAG, "Venmo authorization response: " + response.toString());

        venmoAuthorizationCallbackContext.success(response);
        venmoAuthorizationCallbackContext = null;
      }
    }
  }

  private void btError(Exception error) {
    String developerReadableMessage = error.getMessage();

    Log.d(TAG, "Braintree error: " + developerReadableMessage);

    if (error instanceof AppSwitchNotAvailableException) {
      Log.d(TAG, "Braintree is unable to switch to the Venmo app");

      if (venmoAuthorizationCallbackContext != null) {
        venmoAuthorizationCallbackContext.error(getError(ERROR_AUTHORIZATION_ERROR, "Unable to switch to Venmo app"));
        venmoAuthorizationCallbackContext = null;
      }
    }
  }

  private void btCancel(int requestCode) {
    Log.d(TAG, "User cancelled venmo request: " + requestCode);

    if (venmoAuthorizationCallbackContext != null) {
      venmoAuthorizationCallbackContext.error(getError(ERROR_USER_CANCELLED_AUTHORIZATION));
      venmoAuthorizationCallbackContext = null;
    }
  }

  private JSONObject getError(int errorCode) {
    JSONObject result = new JSONObject();

    try {
      result.put("errorCode", errorCode);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return result;
  }

  private JSONObject getError(int errorCode, String message) {
    JSONObject result = new JSONObject();

    try {
      result.put("errorCode", errorCode);
      result.put("message", message);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return result;
  }
}

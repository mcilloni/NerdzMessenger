
package eu.nerdz.app.authenticator;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NerdzAuthenticatorService extends Service {

    private static final String TAG = "NdzAuthSvc";
    private NerdzAuthenticator mNerdzAuthenticator;

    public IBinder onBind(Intent intent) {

        Log.i(TAG, "onBind(" + intent + ")");

        if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT))
            return this.mNerdzAuthenticator.getIBinder();

        return null;

    }

    public void onCreate() {

        Log.i(TAG, "onCreate()");

        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "NERDZ Authentication Service started.");

        this.mNerdzAuthenticator = new NerdzAuthenticator(this);
    }

    public void onDestroy() {

        Log.i(TAG, "onDestroy()");
        if (Log.isLoggable(TAG, Log.VERBOSE))
            Log.v(TAG, "NERDZ Authentication Service stopped.");
    }
}

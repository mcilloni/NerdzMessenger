
package eu.nerdz.app.messenger.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

import eu.nerdz.api.HttpException;
import eu.nerdz.api.LoginException;
import eu.nerdz.app.messenger.Messaging;
import eu.nerdz.app.messenger.R;

public class SplashScreenActivity extends PopupActivity {

    private static final String TAG = "NdzSplashScreenAct";

    private AccountManager mAM;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_splash_screen);

        Log.d(TAG, "Continuing login in 1 second...");

        this.mAM = AccountManager.get(this);

        new Handler().postDelayed(new Runnable() {

            public void run() {

                SplashScreenActivity.this.keepOnLoggingIn();
            }
        }, 1000L);
    }

    private void keepOnLoggingIn() {

        Log.i(TAG, "keepOnLoggingIn()");

        Account[] accounts = this.mAM.getAccountsByType(this.getString(R.string.account_type));

        if (accounts.length != 1)
            this.mAM.addAccount(this.getString(R.string.account_type), null, null, null, this, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> future) {

                    Log.d(TAG, "Account auth completed");

                    while (!future.isDone())
                        ;

                    try {
                        Bundle result = (Bundle) future.getResult();
                        String userName = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                        String userID = result.getString(SplashScreenActivity.this.getString(R.string.data_nerdzid));
                        String nerdzU = result.getString(SplashScreenActivity.this.getString(R.string.data_nerdzu));

                        SplashScreenActivity.this.launchConversations(userName, userID, nerdzU);

                    } catch (OperationCanceledException e) {
                        Log.d(TAG, "Operation Cancelled.");
                        SplashScreenActivity.this.shortToast("Operation Cancelled.");
                        SplashScreenActivity.this.finish();
                    } catch (IOException e) {
                        Log.e(TAG, "WTF?? IOException:" + e.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, "WTF?? AuthenticatorException:" + e.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                    }
                }
            }, null);
        else {
            String userName = accounts[0].name;
            String userID = this.mAM.getUserData(accounts[0], this.getString(R.string.data_nerdzid));
            String nerdzU = this.mAM.getUserData(accounts[0], this.getString(R.string.data_nerdzu));

            SplashScreenActivity.this.launchConversations(userName, userID, nerdzU);
        }
    }

    private void launchConversations(String userName, String userID, String nerdzU) {

        Log.d(TAG, "userName=" + userName);
        Log.d(TAG, "userID=" + userID);
        Log.d(TAG, "NerdzU=" + nerdzU);

        new AsyncTask<String, Void, Throwable>() {

            @Override
            protected Throwable doInBackground(String... params) {

                try {
                    Messaging.get().init(params[0], params[1], params[2]);
                } catch (Throwable t) {
                    return t;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Throwable result) {

                if (result != null) {
                    if (result instanceof LoginException) {
                        SplashScreenActivity.this.mAM.removeAccount(SplashScreenActivity.this.mAM.getAccountsByType(SplashScreenActivity.this.getString(R.string.account_type))[0], new AccountManagerCallback<Boolean>() {

                            @Override
                            public void run(AccountManagerFuture<Boolean> arg0) {

                                SplashScreenActivity.this.shortToast("Login data is invalid. User account deleted.");
                                SplashScreenActivity.this.keepOnLoggingIn();
                                return;
                            }
                        }, null);
                    } else if (result instanceof IOException) {
                        SplashScreenActivity.this.shortToast("Cannot connect to the server for authentication: " + result.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                        return;
                    } else if (result instanceof HttpException) {
                        SplashScreenActivity.this.shortToast( "Some weird HTTP response received: " + result.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                        return;
                    } else {
                        SplashScreenActivity.this.shortToast("Received a " + result.getClass().toString() + " exception: "+ result.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                        return;
                    }

                } else {
                    Intent intent = new Intent(SplashScreenActivity.this, ConversationsListActivity.class);
                    SplashScreenActivity.this.startActivity(intent);
                }
            }

        }.execute(userName, userID, nerdzU);

    }

}

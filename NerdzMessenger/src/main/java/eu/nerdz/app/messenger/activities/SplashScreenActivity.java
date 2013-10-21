
package eu.nerdz.app.messenger.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.WrongUserInfoTypeException;
import eu.nerdz.app.Keys;
import eu.nerdz.app.messenger.Prefs;
import eu.nerdz.app.messenger.R;

public class SplashScreenActivity extends Activity {

    private static final String TAG = "NdzSplashScreenAct";

    private Nerdz mNerdz = null;

    private AccountManager mAM;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_splash_screen);

        Log.d(TAG, "Continuing login in 1 second...");

        this.mAM = AccountManager.get(this);

        try {
            this.mNerdz = Nerdz.getImplementation(Prefs.getImplementationName());
        } catch (Exception e) {
            this.shortToast(e.getLocalizedMessage());
            this.finish();
        }

        new Handler().postDelayed(new Runnable() {

            public void run() {

                SplashScreenActivity.this.keepOnLoggingIn();
            }
        }, 1000L);
    }

    private void keepOnLoggingIn() {

        Log.i(TAG, "keepOnLoggingIn()");

        Account[] accounts = this.mAM.getAccountsByType(this.getString(R.string.account_type));

        if (accounts.length != 1) {
            this.mAM.addAccount(this.getString(R.string.account_type), null, null, null, this, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> future) {

                    Log.d(TAG, "Account auth completed");

                    while (true)
                        if (future.isDone()) {
                            break;
                        }

                    try {
                        Bundle result = future.getResult();
                        String userData = result.getString(Keys.NERDZ_INFO);
                        SplashScreenActivity.this.launchConversations(SplashScreenActivity.this.mNerdz.deserializeFromString(userData));

                    } catch (OperationCanceledException e) {
                        Log.d(TAG, "Operation Cancelled.");
                        SplashScreenActivity.this.shortToast(SplashScreenActivity.this.getString(R.string.operation_cancelled));
                        SplashScreenActivity.this.finish();
                    } catch (IOException e) {
                        Log.e(TAG, "WTF?? IOException:" + e.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, "WTF?? AuthenticatorException:" + e.getLocalizedMessage());
                        SplashScreenActivity.this.finish();
                    } catch (WrongUserInfoTypeException e) {
                        Log.e(TAG, SplashScreenActivity.this.getString(R.string.api_changed));
                        SplashScreenActivity.this.finish();
                    }
                }
            }, null);
        } else {
            String userData = this.mAM.getUserData(accounts[0], Keys.NERDZ_INFO);

            try {
                SplashScreenActivity.this.launchConversations(SplashScreenActivity.this.mNerdz.deserializeFromString(userData));
            } catch (WrongUserInfoTypeException e) {
                Log.e(TAG, SplashScreenActivity.this.getString(R.string.api_changed));
                SplashScreenActivity.this.finish();
            }
        }
    }

    private void launchConversations(UserInfo userInfo) {

        Log.d(TAG, "userInfo=" + userInfo);

        Intent intent = new Intent(SplashScreenActivity.this, ConversationsListActivity.class);
        intent.putExtra(Keys.NERDZ_INFO, userInfo);
        SplashScreenActivity.this.startActivity(intent);

    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}

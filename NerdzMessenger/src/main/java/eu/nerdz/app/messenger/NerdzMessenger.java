package eu.nerdz.app.messenger;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;

import eu.nerdz.api.InvalidManagerException;
import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.WrongUserInfoTypeException;
import eu.nerdz.app.Keys;

public class NerdzMessenger extends Application {

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String TAG = "NdzMsgStaticAppContext";

    public static final String GCM_SENDER_ID = "";

    private static Context sContext;
    private static AccountManager sAccountManager;
    private static String sUserName;
    private static UserInfo sUserInfo;

    @Override
    public void onCreate(){
        super.onCreate();
        NerdzMessenger.sContext = this.getApplicationContext();
        NerdzMessenger.sAccountManager = AccountManager.get(this);
    }

    public static Context getAppContext() {
        return NerdzMessenger.sContext;
    }

    public static boolean checkPlayServices(final Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, activity, NerdzMessenger.PLAY_SERVICES_RESOLUTION_REQUEST);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        activity.finish();
                    }
                });
                dialog.show();
            } else {
                Log.e("GCMNM", "Cannot continue - no PlayServices availiable");
                NerdzMessenger.gcmNotFoundDialog(activity);
            }

            return false;
        }

        return true;
    }

    private static void ackDialog(final Activity activity, int title, int message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(title).setMessage(message).setNeutralButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }

        });

        AlertDialog dialog = builder.create();

        dialog.show();

    }

    public static AccountManager getAccountManager() {
        return NerdzMessenger.sAccountManager;
    }

    public static int getAppVersion() {
        try {
            return NerdzMessenger.sContext.getPackageManager().getPackageInfo(NerdzMessenger.sContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException failure) {
            throw new DieHorriblyError("Abnormal failure -  " + failure);
        } catch (NullPointerException ignore) {
            throw new DieHorriblyError("NullPointerException while retrieving appVersion");
        }
    }

    public static void gcmNotFoundDialog(Activity activity) {

        NerdzMessenger.ackDialog(activity, R.string.gms_not_found, R.string.gms_not_found_msg);

    }

    public static UserInfo getUserData() {

        if(NerdzMessenger.sUserInfo != null) {
            return NerdzMessenger.sUserInfo;
        }

        Account[] accounts = NerdzMessenger.sAccountManager.getAccountsByType(NerdzMessenger.sContext.getString(R.string.account_type));

        if(accounts.length != 1) {
            throw new DieHorriblyError("No account availiable");
        }

        try {
            Nerdz nerdz = Nerdz.getImplementation(Prefs.getImplementationName());
            String userData = NerdzMessenger.sAccountManager.getUserData(accounts[0], Keys.NERDZ_INFO);
            NerdzMessenger.sUserInfo = nerdz.deserializeFromString(userData);
        } catch (Exception e) {
            throw new DieHorriblyError(e);
        }

        return NerdzMessenger.sUserInfo;

    }

    public static void userData(Activity callingActivity, final AccountAddReaction reaction) {
        Nerdz nerdzJavaIsDumb = null;
        try {
            nerdzJavaIsDumb = Nerdz.getImplementation(Prefs.getImplementationName());
        } catch (Exception e) {
            reaction.onError(e);
            return;
        }

        final Nerdz nerdz = nerdzJavaIsDumb;

        Account[] accounts = NerdzMessenger.sAccountManager.getAccountsByType(NerdzMessenger.sContext.getString(R.string.account_type));

        if (accounts.length != 1) {
            NerdzMessenger.sAccountManager.addAccount(NerdzMessenger.sContext.getString(R.string.account_type), null, null, null, callingActivity, new AccountManagerCallback<Bundle>() {

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
                        NerdzMessenger.sUserInfo = nerdz.deserializeFromString(userData);
                        reaction.onSuccess(NerdzMessenger.sUserInfo);
                    } catch (OperationCanceledException e) {
                        Log.d(TAG, "Operation Cancelled.");
                        reaction.onError(new Exception(NerdzMessenger.sContext.getString(R.string.operation_cancelled)));
                    } catch (IOException e) {
                        Log.e(TAG, "WTF?? IOException:" + e.getLocalizedMessage());
                        reaction.onError(e);
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, "WTF?? AuthenticatorException:" + e.getLocalizedMessage());
                        reaction.onError(e);
                    } catch (WrongUserInfoTypeException e) {
                        Log.e(TAG, NerdzMessenger.sContext.getString(R.string.api_changed));
                        reaction.onError(e);
                    }
                }
            }, null);
        } else {
            String userData = NerdzMessenger.sAccountManager.getUserData(accounts[0], Keys.NERDZ_INFO);
            try {
                NerdzMessenger.sUserInfo = nerdz.deserializeFromString(userData);
                reaction.onSuccess(NerdzMessenger.sUserInfo);
            } catch (WrongUserInfoTypeException e) {
                Log.e(TAG, NerdzMessenger.sContext.getString(R.string.api_changed));
                reaction.onError(e);
            }
        }

    }

    public static String name() {

        if (NerdzMessenger.sUserName != null) {
            return NerdzMessenger.sUserName;
        }

        try {
            Account[] accounts = NerdzMessenger.sAccountManager.getAccountsByType(NerdzMessenger.sContext.getString(R.string.account_type));

            if(accounts.length != 1) {
                throw new DieHorriblyError("No account found");
            }

            NerdzMessenger.sUserName = accounts[0].name;
            return NerdzMessenger.sUserName;
        } catch (NullPointerException ignore) {}
        return "";
    }

    public static interface AccountAddReaction {
        public abstract void onError(Exception message);
        public abstract void onSuccess(UserInfo userData);
    }
}



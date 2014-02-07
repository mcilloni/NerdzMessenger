package eu.nerdz.app.messenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class NerdzMessenger extends Application {

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String TAG = "NdzMsgStaticAppContext";

    public static final String GCM_SENDER_ID = "";

    public static Context context;


    @Override
    public void onCreate(){
        super.onCreate();
        NerdzMessenger.context = this.getApplicationContext();
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

    public static int getAppVersion() {
        try {
            return NerdzMessenger.context.getPackageManager().getPackageInfo(NerdzMessenger.context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException failure) {
            throw new DieHorriblyError("Abnormal failure -  " + failure);
        } catch (NullPointerException ignore) {
            throw new DieHorriblyError("NullPointerException while retrieving appVersion");
        }
    }

    public static void gcmNotFoundDialog(Activity activity) {

        NerdzMessenger.ackDialog(activity, R.string.gms_not_found, R.string.gms_not_found_msg);

    }


}



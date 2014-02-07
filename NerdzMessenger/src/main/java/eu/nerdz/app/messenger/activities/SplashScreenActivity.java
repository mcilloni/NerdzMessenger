
package eu.nerdz.app.messenger.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import eu.nerdz.api.UserInfo;
import eu.nerdz.app.messenger.NerdzMessenger;
import eu.nerdz.app.messenger.Prefs;
import eu.nerdz.app.messenger.R;
import eu.nerdz.app.messenger.Server;


public class SplashScreenActivity extends Activity {


    private static final String TAG = "NdzSplashScreenAct";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        if (!NerdzMessenger.checkPlayServices(this)) {
            return;
        }
        this.setContentView(R.layout.layout_splash_screen);

        this.showDialog(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "Continuing login in 1 second...");

                new Handler().postDelayed(new Runnable() {

                    public void run() {

                        SplashScreenActivity.this.keepOnLoggingIn();
                    }
                }, 1000L);
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        NerdzMessenger.checkPlayServices(this);
    }


    private void keepOnLoggingIn() {

        Log.i(TAG, "keepOnLoggingIn()");

        Server.getInstance().userData(this, new Server.AccountAddReaction() {
            @Override
            public void onError(Exception message) {
                SplashScreenActivity.this.shortToast(message.getLocalizedMessage());
                SplashScreenActivity.this.finish();
            }

            @Override
            public void onSuccess(UserInfo userData) {
                SplashScreenActivity.this.launchConversations(userData);
            }
        });
    }

    private void launchConversations(UserInfo userInfo) {

        Log.d(TAG, "userInfo=" + userInfo);

        Intent intent = new Intent(SplashScreenActivity.this, ConversationsListActivity.class);
        SplashScreenActivity.this.startActivity(intent);

    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showDialog(final Runnable runnable) {

        if(!Prefs.accepted()) {

            AlertDialog.Builder builder;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
            } else {
                builder = new AlertDialog.Builder(this);
            }

            builder.setMessage(R.string.conditions)
                   .setCancelable(false)
                   .setNegativeButton(R.string.disagree, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           SplashScreenActivity.this.finish();
                       }
                   })
                   .setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           Prefs.setAccepted();
                           runnable.run();
                       }
                   })
                   .setTitle(R.string.conditions_title);
            builder.create().show();
        } else {
            runnable.run();
        }
    }

}

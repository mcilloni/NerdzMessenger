package eu.nerdz.app.messenger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import eu.nerdz.api.UserInfo;
import eu.nerdz.app.messenger.activities.SplashScreenActivity;

public class LogoutDialog extends DialogPreference {
    public LogoutDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            AccountManager am = AccountManager.get(NerdzMessenger.context);
            Account account = am.getAccountsByType(NerdzMessenger.context.getString(R.string.account_type))[0];
            am.removeAccount(account, new AccountManagerCallback<Boolean>() {

                @Override
                public void run(AccountManagerFuture<Boolean> future) {

                    while (true) {
                        if (future.isDone()) {
                            break;
                        }
                    }

                    Server.getInstance().unregisterGcmUser(new Server.Reaction() {
                        @Override
                        public void onError(Exception message) {
                            NerdzMessenger.shortToast(message.getLocalizedMessage());
                        }

                        @Override
                        public void onSuccess(UserInfo userData) {}
                    });

                    NerdzMessenger.shortToast(R.string.account_deleted);

                    Intent intent = new Intent(NerdzMessenger.context, SplashScreenActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    NerdzMessenger.context.startActivity(intent);

                }
            }, null);
        }
    }
}

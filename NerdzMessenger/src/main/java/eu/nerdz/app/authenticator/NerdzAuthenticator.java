
package eu.nerdz.app.authenticator;

import java.lang.ref.WeakReference;

import eu.nerdz.app.messenger.R;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

class NerdzAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = "NdzAuthenticator";

    private Context mContext;
    private final MsgHandler mHandler;

    private static class MsgHandler extends Handler {

        private final WeakReference<Context> context;

        public MsgHandler(Context context) {

            this.context = new WeakReference<Context>(context);

        }

        @Override
        public void handleMessage(Message paramAnonymousMessage) {

            if (paramAnonymousMessage.arg1 == 1)
                Toast.makeText(this.context.get().getApplicationContext(), R.string.error_account_already_present, Toast.LENGTH_SHORT).show();

        }
    };

    public NerdzAuthenticator(Context context) {

        super(context);
        this.mContext = context;
        this.mHandler = new MsgHandler(context);

    }

    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {

        Log.v(TAG, "addAccount()");
        Intent intent = new Intent(this.mContext, LoginActivity.class);
        intent.putExtra("accountAuthenticatorResponse", response);
        Bundle result = new Bundle();

        if (AccountManager.get(this.mContext).getAccountsByType(this.mContext.getString(R.string.account_type)).length != 0) {
            Log.d(TAG, "addAccount() rejected, an account is already present");
            Message message = this.mHandler.obtainMessage();
            message.arg1 = 1;
            this.mHandler.sendMessage(message);
            Log.d(TAG, "message sent.");
            result.putString("errorMessage", this.mContext.getString(R.string.error_account_already_present));
            return result;
        }

        result.putParcelable("intent", intent);
        return result;
    }

    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {

        Log.v(TAG, "confirmCredentials() - unsupported");
        return null;
    }

    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {

        Log.v(TAG, "editProperties()");
        throw new UnsupportedOperationException();
    }

    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        Log.v(TAG, "getAuthToken()");
        return null;
    }

    public String getAuthTokenLabel(String authTokenType) {

        Log.v(TAG, "getAuthTokenLabel()");
        return null;
    }

    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {

        Log.v(TAG, "hasFeatures()");
        return null;
    }

    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        Log.v(TAG, "getAuthTokenLabel()");
        return null;
    }
}

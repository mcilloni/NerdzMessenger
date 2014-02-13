
package eu.nerdz.app.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

import eu.nerdz.api.ContentException;
import eu.nerdz.api.InvalidManagerException;
import eu.nerdz.api.LoginException;
import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.WrongUserInfoTypeException;
import eu.nerdz.api.messages.Messenger;
import eu.nerdz.app.Keys;
import eu.nerdz.app.messenger.NerdzMessenger;
import eu.nerdz.app.messenger.Prefs;
import eu.nerdz.app.messenger.R;

public class LoginActivity extends ActionBarActivity {


    private static final String TAG = "NdzLoginAct";
    private AccountManager mAccountManager;
    private UserLoginTask mAuthTask = null;
    private View mLoginFormView;
    private TextView mLoginStatusMessageView;
    private View mLoginStatusView;
    private String mPassword;
    private EditText mPasswordView;
    private String mUsername;
    private EditText mUsernameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        this.mAccountManager = AccountManager.get(this);

        this.setContentView(R.layout.layout_login);

        this.mUsernameView = ((EditText) findViewById(R.id.username));
        this.mUsernameView.setText(this.mUsername);

        this.mPasswordView = ((EditText) findViewById(R.id.password));
        this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView paramAnonymousTextView, int paramAnonymousInt, KeyEvent paramAnonymousKeyEvent) {

                if ((paramAnonymousInt == R.id.login) || (paramAnonymousInt == EditorInfo.IME_NULL)) {
                    LoginActivity.this.attemptLogin();
                    return true;
                }
                return false;
            }
        });
        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {

                LoginActivity.this.mPasswordView.setError(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
        };
        this.mPasswordView.addTextChangedListener(textWatcher);
        this.mUsernameView.addTextChangedListener(textWatcher);

        this.mLoginFormView = findViewById(R.id.login_form);
        this.mLoginStatusView = findViewById(R.id.login_status);
        this.mLoginStatusMessageView = ((TextView) findViewById(R.id.login_status_message));
        this.findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {

            public void onClick(View paramAnonymousView) {

                LoginActivity.this.attemptLogin();
            }
        });
    }

    private void popUp(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        try {
            builder.setMessage(msg);
            builder.create().show();
        } finally {}
    }
    
    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Shows the progress UI and hides the login form.
     */
    @SuppressLint("Override")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = this.getResources().getInteger(android.R.integer.config_shortAnimTime);

            this.mLoginStatusView.setVisibility(View.VISIBLE);
            this.mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    LoginActivity.this.mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            this.mLoginFormView.setVisibility(View.VISIBLE);
            this.mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    LoginActivity.this.mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            this.mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            this.mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void attemptLogin() {

        Log.i(TAG, "attemptLogin()");

        // Hide input method from screen.
        ((InputMethodManager) this.getSystemService("input_method")).hideSoftInputFromWindow(this.getWindow().getCurrentFocus().getWindowToken(), 0);

        if (this.mAuthTask != null)
            return;

        this.mUsernameView.setError(null);
        this.mPasswordView.setError(null);

        this.mUsername = this.mUsernameView.getText().toString();
        this.mPassword = this.mPasswordView.getText().toString();

        View focusView = null;
        boolean cancel = false;

        if (TextUtils.isEmpty(this.mPassword)) {
            this.mPasswordView.setError(this.getString(R.string.error_field_required));
            focusView = this.mPasswordView;
            cancel = true;
        } else if (this.mPassword.length() < 4) {
            this.mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = this.mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(this.mUsername)) {
            this.mUsernameView.setError(this.getString(R.string.error_field_required));
            focusView = this.mUsernameView;
            cancel = true;
        }

        if (cancel)
            focusView.requestFocus();
        else {

            this.mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            this.showProgress(true);
            this.mAuthTask = new UserLoginTask();

            String[] userData = new String[] {this.mUsername, this.mPassword};

            this.mAuthTask.execute(userData);
        }
    }

    public class UserLoginTask extends AsyncTask<String, Void, Pair<UserInfo,Throwable>> {

        @Override
        protected Pair<UserInfo,Throwable> doInBackground(String... params) {

            Log.i(TAG, "logging in...");

            try {

                Nerdz nerdz = Nerdz.getImplementation(Prefs.getImplementationName());
                UserInfo info = nerdz.logAndGetInfo(params[0], params[1]);

                return Pair.create(info, null);
            } catch (Throwable t) {
                return Pair.create(null,t);
            }
        }

        @Override
        protected void onCancelled() {

            LoginActivity.this.mAuthTask = null;
            LoginActivity.this.showProgress(false);
        }

        @Override
        protected void onPostExecute(Pair<UserInfo,Throwable> result) {

            Log.i(TAG, "onPostExecute()");

            LoginActivity.this.showProgress(false);
            LoginActivity.this.mAuthTask = null;

            Throwable throwable = result.second;

            if (throwable != null) {
                if ((throwable instanceof LoginException)) {
                    LoginActivity.this.mPasswordView.setError(LoginActivity.this.getString(R.string.error_invalid_login));
                    LoginActivity.this.mPasswordView.requestFocus();
                    return;
                }

                Log.w(TAG, throwable);

                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + throwable.getClass() + ": " + throwable.getLocalizedMessage());
                return;
            }
            
            Log.d(TAG, "No exception raised . Going ahead...");

            UserInfo userInfo = result.first;
            String userName = userInfo.getUsername();
            Account account = new Account(userName, LoginActivity.this.getString(R.string.account_type));
            
            try {
                Nerdz nerdz = Nerdz.getImplementation(Prefs.getImplementationName());
                Bundle userData = new Bundle();
                userData.putString(Keys.NERDZ_INFO, nerdz.serializeToString(userInfo));
                boolean created = LoginActivity.this.mAccountManager.addAccountExplicitly(account, null, userData);
                Bundle extras = LoginActivity.this.getIntent().getExtras();
                
                if (extras != null) {
                    Log.d(TAG, "account" + userName + "created: " + created);
                    
                    if (created) {
                        
                        AccountAuthenticatorResponse response = extras.getParcelable("accountAuthenticatorResponse");
                        
                        Bundle operationResult = new Bundle();
                        operationResult.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                        operationResult.putString(AccountManager.KEY_ACCOUNT_TYPE, LoginActivity.this.getString(R.string.account_type));
                        operationResult.putString(Keys.NERDZ_INFO, nerdz.serializeToString(userInfo));
                        response.onResult(operationResult);
                        
                        Log.d(TAG, "showing a Toast...");
                        
                        String msg = String.format(LoginActivity.this.getString(R.string.login_successful), userName);
                        LoginActivity.this.shortToast(msg);
                    }
                }
                
                Log.d(TAG, "onPostExecute() has finished correctly.");
                LoginActivity.this.finish();
                
            } catch (ContentException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            } catch (ClassNotFoundException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            } catch (InvalidManagerException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            } catch (InstantiationException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            } catch (WrongUserInfoTypeException e) {
                LoginActivity.this.popUp("OH NOES EXCEPTIONZ!!!!1111!\n" + e.getLocalizedMessage());
            }
        }
    }
}


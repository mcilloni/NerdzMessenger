package eu.nerdz.app.messenger.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import eu.nerdz.api.BadStatusException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.messages.Message;
import eu.nerdz.api.messages.Messenger;
import eu.nerdz.app.Keys;
import eu.nerdz.app.messenger.DieHorriblyError;
import eu.nerdz.app.messenger.NerdzMessenger;
import eu.nerdz.app.messenger.Prefs;
import eu.nerdz.app.messenger.R;
import eu.nerdz.app.messenger.Server;

public class NewMessageActivity extends ActionBarActivity {

    private static final String TAG = "NdzNewMessAct";

    View mMessageSendView;
    View mMessageSendLayout;
    EditText mMessageBox, mTo;
    Button mButton;
    Message mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_new_message);

        this.mMessageSendView = this.findViewById(R.id.message_send);
        this.mMessageSendLayout = this.findViewById(R.id.message_send_layout);

        this.mMessageBox = (EditText) this.findViewById(R.id.new_message_text);
        this.mMessageBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    NewMessageActivity.this.mButton.setText(R.string.lol);
                } else {
                    NewMessageActivity.this.mButton.setText(R.string.send);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });

        this.mTo = (EditText) this.findViewById(R.id.send_to);
        this.mTo.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                NewMessageActivity.this.mButton.setEnabled(!TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });

        this.mButton = (Button) this.findViewById(R.id.send_button);
        this.mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MessageSender messageSender = new MessageSender();

                String text = NewMessageActivity.this.mMessageBox.getText().toString();

                if (TextUtils.isEmpty(text)) {
                    text = "LOL";
                }

                try {
                    ((InputMethodManager) NewMessageActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(NewMessageActivity.this.getWindow().getCurrentFocus().getWindowToken(), 0);
                } catch (NullPointerException ignore) {
                } //Ignore; nobody will get hurt from this.

                NewMessageActivity.this.showProgress(true);

                messageSender.execute(NewMessageActivity.this.mTo.getText().toString(), text);

            }
        });
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_new_message, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        NerdzMessenger.checkPlayServices(this);
    }

    /**
     * Shows the progress UI, or hides it
     */
    @SuppressLint("Override")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = this.getResources().getInteger(android.R.integer.config_shortAnimTime);

            this.mMessageSendView.setVisibility(View.VISIBLE);
            this.mMessageSendView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    NewMessageActivity.this.mMessageSendView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });


            this.mMessageSendLayout.setVisibility(View.VISIBLE);
            this.mMessageSendLayout.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    NewMessageActivity.this.mMessageSendLayout.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            this.mMessageSendView.setVisibility(show ? View.VISIBLE : View.GONE);
            this.mMessageSendLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void shortToast(int id) {
        this.shortToast(this.getString(id));
    }

    private void longToast(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void longToast(int id) {

        this.longToast(this.getString(id));
    }

    private class MessageSender extends AsyncTask<String, Void, Pair<Message, Throwable>> {

        @Override
        protected Pair<Message, Throwable> doInBackground(String... params) {
            try {
                return Pair.create(Server.getInstance().sendMessage(params[0], params[1]), null);
            } catch (Throwable t) {
                return Pair.create(null, t);
            }
        }

        @Override
        protected void onPostExecute(Pair<Message, Throwable> result) {

            Throwable t = result.second;

            if (t != null) {

                Log.d(TAG, Log.getStackTraceString(t));

                if (t instanceof BadStatusException) {
                    NewMessageActivity.this.longToast(R.string.cant_send);
                } else if (t instanceof IOException) {
                    NewMessageActivity.this.shortToast("Network error: " + t.getLocalizedMessage());
                } else if (t instanceof HttpException) {
                    NewMessageActivity.this.shortToast("HTTP Error: " + t.getLocalizedMessage());
                } else {
                    NewMessageActivity.this.shortToast("Exception: " + t.getLocalizedMessage());
                }
                NewMessageActivity.this.showProgress(false);
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra(Keys.OPERATION_RESULT, ConversationsListActivity.Result.REFRESH);
            NewMessageActivity.this.setResult(Activity.RESULT_OK, resultIntent);

            NewMessageActivity.this.finish();

        }

    }


}


package eu.nerdz.app.messenger.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.nerdz.api.ContentException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.messages.Conversation;
import eu.nerdz.api.messages.Message;
import eu.nerdz.app.messenger.Messaging;
import eu.nerdz.app.messenger.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class ConversationsListActivity extends ActionBarActivity {

    public static final String TAG = "NdzConvListAct";

    private View mConversationsListView;
    private View mFetchStatusView;
    private View mNoConversationsMsgView;
    private ConversationFetch mConversationFetch;

    List<Pair<Conversation, Message>> mConversations;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        boolean noSavedState = savedInstanceState == null;

        if (noSavedState)
            this.setContentView(R.layout.layout_conversations_list);

        this.mConversationsListView = this.findViewById(R.id.conversations_list);
        this.mFetchStatusView = this.findViewById(R.id.fetch_status);
        this.mNoConversationsMsgView = this.findViewById(R.id.no_conversations_msg);

        if (noSavedState)
            this.fetchConversations();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        this.mConversationsListView = this.findViewById(R.id.conversations_list);
        this.mFetchStatusView = this.findViewById(R.id.fetch_status);
        this.mNoConversationsMsgView = this.findViewById(R.id.no_conversations_msg);

        super.onConfigurationChanged(newConfig);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        this.getMenuInflater().inflate(R.menu.menu_conversations_list, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.delete_account:
                AccountManager am = AccountManager.get(this);
                Account account = am.getAccountsByType(this.getString(R.string.account_type))[0];
                am.removeAccount(account, new AccountManagerCallback<Boolean>() {

                    @Override
                    public void run(AccountManagerFuture<Boolean> future) {

                        while (!future.isDone());

                        ConversationsListActivity.this.shortToast(ConversationsListActivity.this.getString(R.string.account_deleted));

                        Intent intent = new Intent(ConversationsListActivity.this, SplashScreenActivity.class);
                        ConversationsListActivity.this.startActivity(intent);
                        ConversationsListActivity.this.finish();

                    }
                }, null);
                return true;
            case R.id.refresh_button:
                this.fetchConversations();
                return true;
        default:
            return super.onOptionsItemSelected(item);
        }

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

            this.mFetchStatusView.setVisibility(View.VISIBLE);
            this.mFetchStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    ConversationsListActivity.this.mFetchStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });

            if (!show) {
                final View ourView = (this.mConversations == null ? this.mNoConversationsMsgView : this.mConversationsListView);
                ourView.setVisibility(View.VISIBLE);
                ourView.animate().setDuration(shortAnimTime).alpha(1).setListener(new AnimatorListenerAdapter() {

                    @SuppressLint("Override")
                    public void onAnimationEnd(Animator animation) {

                        ourView.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                final View ourView = (this.mNoConversationsMsgView.getVisibility() == View.VISIBLE ? this.mNoConversationsMsgView : this.mConversationsListView);
                ourView.setVisibility(View.VISIBLE);
                ourView.animate().setDuration(shortAnimTime).alpha(0).setListener(new AnimatorListenerAdapter() {

                    @SuppressLint("Override")
                    public void onAnimationEnd(Animator animation) {

                        ourView.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            this.mFetchStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) {
                final View ourView = (this.mConversations == null ? this.mNoConversationsMsgView : this.mConversationsListView);
                ourView.setVisibility(View.VISIBLE);
            } else {
                final View ourView = (this.mNoConversationsMsgView.getVisibility() == View.VISIBLE ? this.mNoConversationsMsgView : this.mConversationsListView);
                ourView.setVisibility(View.GONE);
            }
        }
    }

    private void fetchConversations() {

        Log.d(TAG, "fetchConversations()");

        this.showProgress(true);
        if (this.mConversationFetch != null && this.mConversationFetch.getStatus() == AsyncTask.Status.RUNNING) {
            this.mConversationFetch.cancel(true);
        }
        (this.mConversationFetch = new ConversationFetch()).execute();

    }

    private void updateList() {

        Log.d(TAG, "updateList()");

        ConversationsListActivity.this.showProgress(false);

    }

    private void shortToast(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class ConversationFetch extends AsyncTask<Void, Void, Pair<List<Pair<Conversation, Message>>, Throwable>> {

        private Messaging mMessaging;

        public ConversationFetch() {

            this.mMessaging = Messaging.get();
        }

        @Override
        protected Pair<List<Pair<Conversation, Message>>, Throwable> doInBackground(Void... params) {

            Log.d(TAG, "doInBackground()");

            try {
                List<Conversation> conversations = this.mMessaging.getConversations();
                if (conversations == null)
                    return null;
                List<Pair<Conversation, Message>> newList = new ArrayList<Pair<Conversation, Message>>(conversations.size());
                for (Conversation conversation : conversations) {
                    Message sample = this.mMessaging.getFirstMessage(conversation);
                    newList.add(Pair.create(conversation, sample));
                }
                return Pair.create(newList, null);
            } catch (Throwable t) {
                return Pair.create(null, t);
            }

        }

        @Override
        protected void onCancelled() {
            ConversationsListActivity.this.mConversations = null;
            ConversationsListActivity.this.showProgress(false);
        }

        @Override
        protected void onPostExecute(Pair<List<Pair<Conversation, Message>>, Throwable> result) {

            Log.d(TAG, "onPostExecute()");

            Throwable t = result.second;

            if (t != null) {
                Log.d(TAG, "received a " + t.getClass().toString() + " throwable");

                if (t instanceof ContentException)
                    ConversationsListActivity.this.shortToast("There's something weird in NERDZ Beta. Please, blame Robertof ASAP: " + t.getLocalizedMessage());
                else if (t instanceof IOException)
                    ConversationsListActivity.this.shortToast("Network error: " + t.getLocalizedMessage());
                else if (t instanceof HttpException)
                    ConversationsListActivity.this.shortToast("HTTP Error: " + t.getLocalizedMessage());
                else
                    ConversationsListActivity.this.shortToast("Exception: " + t.getLocalizedMessage());
                ConversationsListActivity.this.finish();
                return;
            }

            // ConversationsListActivity.this.mConversations = result.first;

            ConversationsListActivity.this.updateList();
        }

    }

}

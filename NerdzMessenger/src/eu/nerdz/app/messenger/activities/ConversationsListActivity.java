
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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;

public class ConversationsListActivity extends PopupActivity {

    public static final String TAG = "NdzConvListAct";

    private View mConversationsListView;
    private View mFetchStatusView;
    private View mNoConversationsMsgView;

    //private boolean mToggled;
    List<Pair<Conversation, Message>> mConversations;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate(" + savedInstanceState + ")");

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.layout_conversations_list);

       // this.mToggled = false;

        this.mConversationsListView = this.findViewById(R.id.conversations_list);
        this.mFetchStatusView = this.findViewById(R.id.fetch_status);
        this.mNoConversationsMsgView = this.findViewById(R.id.no_conversations_msg);

        this.fetchConversations();

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
        new ConversationFetch().execute();

    }

//    private void toggleNoConversationsVisibility(boolean show) {
//
//        // if show is equal to the current status, this function is unnecessary.
//        if (this.mToggled ^ show) {
//
//            this.mNoConversationsMsgView.setVisibility(show ? View.VISIBLE : View.GONE);
//            ;
//            this.mConversationsListView.setVisibility(show ? View.GONE : View.VISIBLE);
//
//            this.mToggled = show;
//
//            Log.d(TAG, "visibility of no conversation textview: " + (this.mNoConversationsMsgView.getVisibility() == View.VISIBLE));
//
//        }
//
//    }

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

            //ConversationsListActivity.this.mConversations = result.first;

            // if (conversations == null) {

            ConversationsListActivity.this.showProgress(false);
            // } else {
            // ConversationsListActivity.this.toggleNoConversationsVisibility(false);
            // }

        }

    }

}

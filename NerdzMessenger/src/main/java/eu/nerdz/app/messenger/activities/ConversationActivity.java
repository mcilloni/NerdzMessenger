package eu.nerdz.app.messenger.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import eu.nerdz.api.BadStatusException;
import eu.nerdz.api.ContentException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.InvalidManagerException;
import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.messages.Conversation;
import eu.nerdz.api.messages.Message;
import eu.nerdz.api.messages.Messenger;
import eu.nerdz.app.messenger.DieHorriblyError;
import eu.nerdz.app.messenger.Prefs;
import eu.nerdz.app.messenger.R;

public class ConversationActivity extends ActionBarActivity {

    private final static String TAG = "NdzConvAct";
    UserInfo mUserInfo;
    Conversation mThisConversation;
    LinkedList<Message> mMessages;
    Messenger mMessenger;

    ListView mListView;
    View mConversationFetchView;
    View mConversationLayoutView;
    EditText mMessageBox;

    MessageFetch mMessageFetch;
    ConversationAdapter mConversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate(" + savedInstanceState + ')');

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_conversation);

        this.mMessages = new LinkedList<Message>();

        Intent intent = this.getIntent();

        this.mUserInfo = (UserInfo) intent.getSerializableExtra(this.getString(R.string.data_nerdzinfo));
        this.mThisConversation = (Conversation) intent.getSerializableExtra(this.getString(R.string.selected_item));

        if (this.mUserInfo == null || this.mThisConversation == null) {

            this.shortToast(R.string.wrong_parameters);

            throw new DieHorriblyError("Wrong parameters for this activity");
        }

        try {
            this.mMessenger = Nerdz.getImplementation(Prefs.getImplementationName()).restoreMessenger(ConversationActivity.this.mUserInfo);
        } catch (Throwable t) {
            this.longToast("Caught an exception in a place where can't be one: " + t.getLocalizedMessage());
            throw new DieHorriblyError(t.getLocalizedMessage());
        }

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(this.mThisConversation.getOtherName());

        this.mConversationFetchView = this.findViewById(R.id.conversation_fetch);
        this.mConversationLayoutView = this.findViewById(R.id.conversation_layout);

        this.mConversationAdapter = new ConversationAdapter(this.mMessages);

        this.mListView = (ListView) this.findViewById(R.id.conversation);
        this.mListView.setAdapter(this.mConversationAdapter);

        this.mMessageBox = (EditText) this.findViewById(R.id.new_message_text);

        ((Button) this.findViewById(R.id.send_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MessageSender messageSender = new MessageSender();

                String text = ConversationActivity.this.mMessageBox.getText().toString();

                if(TextUtils.isEmpty(text)) {
                    text = "LOL";
                }

                try {
                    ((InputMethodManager) ConversationActivity.this.getSystemService("input_method")).hideSoftInputFromWindow(ConversationActivity.this.getWindow().getCurrentFocus().getWindowToken(), 0);
                } catch (NullPointerException e) {} //Ignore; nobody will get hurt from this.

                ConversationActivity.this.showProgress(true);

                messageSender.execute(text);

            }
        });

        this.getMessages();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.finish();
                return true;

            case R.id.msgs_refresh_button:
                this.getMessages();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollDownList() {
        this.mListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                ConversationActivity.this.mListView.setSelection(ConversationActivity.this.mConversationAdapter.getCount() - 1);
            }
        });
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

            this.mConversationFetchView.setVisibility(View.VISIBLE);
            this.mConversationFetchView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    ConversationActivity.this.mConversationFetchView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });


            this.mConversationLayoutView.setVisibility(View.VISIBLE);
            this.mConversationLayoutView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {

                @SuppressLint("Override")
                public void onAnimationEnd(Animator animation) {

                    ConversationActivity.this.mConversationLayoutView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            this.mConversationFetchView.setVisibility(show ? View.VISIBLE : View.GONE);
            this.mConversationLayoutView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void getMessages() {

        Log.d(TAG, "getMessages()");

        this.showProgress(true);
        if (this.mMessageFetch != null && this.mMessageFetch.getStatus() == AsyncTask.Status.RUNNING) {
            this.mMessageFetch.cancel(true);
        }
        (this.mMessageFetch = new MessageFetch()).execute(0, 10);

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

    static String formatDate(Date date, Context context) {

        Calendar now = Calendar.getInstance();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        Calendar ourDate = Calendar.getInstance();
        ourDate.setTime(date);

        String buffer = (yesterday.get(Calendar.YEAR) == ourDate.get(Calendar.YEAR) &&
                         yesterday.get(Calendar.DAY_OF_YEAR) == ourDate.get(Calendar.DAY_OF_YEAR))
                        ? context.getString(R.string.yesterday) + ", "
                        : (now.get(Calendar.YEAR) == ourDate.get(Calendar.YEAR) &&
                           now.get(Calendar.DAY_OF_YEAR) == ourDate.get(Calendar.DAY_OF_YEAR))
                          ? ""
                          : DateFormat.getDateInstance().format(date) + ", ";

        buffer += DateFormat.getTimeInstance().format(date);

        return buffer;

    }

    static class ViewHolder {

        public TextView message, date;

        public ViewHolder(TextView message, TextView date) {
            this.message = message;
            this.date = date;
        }
    }

    private class MessageFetch extends AsyncTask<Integer, Void, Pair<List<Message>, Throwable>> {

        @Override
        protected Pair<List<Message>, Throwable> doInBackground(Integer... params) {

            Log.d(TAG, "doInBackground()");

            try {
                return Pair.create(ConversationActivity.this.mMessenger.getConversationHandler().getMessages(ConversationActivity.this.mThisConversation, params[0], params[1]), null);
            } catch (Throwable t) {
                return Pair.create(null, t);
            }
        }

        @Override
        protected void onCancelled() {
            ConversationActivity.this.showProgress(false);
        }

        @Override
        protected void onPostExecute(Pair<List<Message>, Throwable> result) {

            Log.d(TAG, "onPostExecute()");

            Throwable t = result.second;

            if (t != null) {

                Log.e(TAG, Log.getStackTraceString(t));

                if (t instanceof ContentException) {
                    ConversationActivity.this.longToast("There's something weird in NERDZ Beta. Please, blame Robertof ASAP: " + t.getLocalizedMessage());
                } else if (t instanceof IOException) {
                    ConversationActivity.this.shortToast("Network error: " + t.getLocalizedMessage());
                } else if (t instanceof HttpException) {
                    ConversationActivity.this.shortToast("HTTP Error: " + t.getLocalizedMessage());
                } else if (t instanceof InvalidManagerException) {
                    ConversationActivity.this.shortToast("Corrupted data/implementation: " + t.getLocalizedMessage());
                } else {
                    ConversationActivity.this.shortToast("Exception: " + t.getLocalizedMessage());
                }
                ConversationActivity.this.finish();
                return;
            }

            ConversationActivity.this.showProgress(false);

            ConversationActivity.this.mMessages.addAll(result.first);
            ConversationActivity.this.mConversationAdapter.notifyDataSetChanged();

            ConversationActivity.this.scrollDownList();


        }
    }



    private class MessageSender extends AsyncTask<String, Void, Pair<Message, Throwable>> {

        @Override
        protected Pair<Message, Throwable> doInBackground(String... params) {
            try {
                return Pair.create(ConversationActivity.this.mMessenger.sendMessage(ConversationActivity.this.mThisConversation.getOtherName(), params[0]), null);
            } catch (Throwable t) {
                return Pair.create(null, t);
            }
        }

        @Override
        protected void onPostExecute(Pair<Message, Throwable> result) {

            Throwable t = result.second;

            ConversationActivity.this.showProgress(false);

            if(t != null) {

                Log.d(TAG, Log.getStackTraceString(t));

                if (t instanceof BadStatusException) {
                    ConversationActivity.this.shortToast(R.string.antiflood_wait);
                } else {
                    if (t instanceof IOException) {
                        ConversationActivity.this.shortToast("Network error: " + t.getLocalizedMessage());
                    } else if (t instanceof HttpException) {
                        ConversationActivity.this.shortToast("HTTP Error: " + t.getLocalizedMessage());
                    } else {
                        ConversationActivity.this.shortToast("Exception: " + t.getLocalizedMessage());
                    }

                    ConversationActivity.this.finish();

                }

                return;

            }

            ConversationActivity.this.mMessages.add(result.first);
            ConversationActivity.this.mConversationAdapter.notifyDataSetChanged();

            ConversationActivity.this.scrollDownList();

            ConversationActivity.this.mMessageBox.setText("");

        }

    }

    private class ConversationAdapter extends ArrayAdapter<Message> {

        private List<Message> mMessages;
        private ActionBarActivity mActivity;

        public ConversationAdapter(List<Message> messages) {
            super(ConversationActivity.this, R.layout.conversation_message, messages);

            this.mMessages = messages;
            this.mActivity = ConversationActivity.this;

        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {

            Message element = this.mMessages.get(position);

            ViewHolder tag;

            if (rowView == null) {
                LayoutInflater layoutInflater = this.mActivity.getLayoutInflater();
                rowView = layoutInflater.inflate(R.layout.conversation_message, null);
                tag = new ViewHolder(
                        (TextView) rowView.findViewById(R.id.message_text_view),
                        (TextView) rowView.findViewById(R.id.message_received_date)
                );
                rowView.setTag(tag);
            } else {
                tag = (ViewHolder) rowView.getTag();
            }

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tag.message.getLayoutParams();

            if (element.getSenderID() != ConversationActivity.this.mUserInfo.getNerdzID()) {
                tag.date.setGravity(Gravity.LEFT);
                tag.message.setGravity(Gravity.LEFT);
                layoutParams.setMargins(0, 0, this.dpToPixels(30), 0);
                rowView.setBackgroundColor(0xFFFAFAFA);

            } else {
                tag.date.setGravity(Gravity.RIGHT);
                tag.message.setGravity(Gravity.RIGHT);
                layoutParams.setMargins(this.dpToPixels(30), 0, 0, 0);
                rowView.setBackgroundColor(0xFFF3F3F3);
            }

            tag.message.setLayoutParams(layoutParams);

            tag.message.setText(element.getContent());
            Linkify.addLinks(tag.message, Linkify.ALL);

            tag.date.setText(ConversationActivity.formatDate(element.getDate(), this.mActivity));

            return rowView;
        }

        private int dpToPixels(int dp) {
            Resources r = this.mActivity.getResources();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        }

    }

}

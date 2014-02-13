package eu.nerdz.app.messenger.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
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
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.integralblue.httpresponsecache.HttpResponseCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.nerdz.api.BadStatusException;
import eu.nerdz.api.ContentException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.InvalidManagerException;
import eu.nerdz.api.messages.Message;
import eu.nerdz.api.messages.MessageFetcher;
import eu.nerdz.app.Keys;
import eu.nerdz.app.messenger.GcmIntentService;
import eu.nerdz.app.messenger.NerdzMessenger;
import eu.nerdz.app.messenger.activities.ConversationsListActivity.Result;
import eu.nerdz.app.messenger.DieHorriblyError;
import eu.nerdz.app.messenger.R;
import eu.nerdz.app.messenger.Server;

public class ConversationActivity extends NerdzMessengerActivity {

    private final static String TAG = "NdzConvAct";
    MessageFetcher mThisConversation;
    LinkedList<Message> mMessages, mSent;

    boolean mStashed;

    ListView mListView;
    View mConversationLayoutView;
    EditText mMessageBox;
    Button mButton, mMoreButton;

    MessageFetch mMessageFetch;
    ConversationAdapter mConversationAdapter;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate(" + savedInstanceState + ')');

        super.onCreate(savedInstanceState);

        this.supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        this.setContentView(R.layout.layout_conversation);

        this.unsetNotification();

        this.mMessages = new LinkedList<Message>();
        this.mSent = new LinkedList<Message>();

        Intent intent = this.getIntent();

        final String from = intent.getStringExtra(Keys.FROM);
        final int id = intent.getIntExtra(Keys.FROM_ID, -1);

        if (from == null || id < 0) {

            this.shortToast(R.string.wrong_parameters);

            throw new DieHorriblyError("Wrong parameters for this activity");
        }

        try {
            this.mThisConversation = Server.getInstance().getFetcher(from, id);
        } catch (Exception e) {
            this.shortToast("Fatal error: broken API");
            this.finish();
        }

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(this.mThisConversation.getOtherName());

        //We need a layout that wraps the button and compresses when the button is hidden. Android is stupid and this is an horrible workaround for this.
        FrameLayout headerLayout = new FrameLayout(this);
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLayout.setLayoutParams(layoutParams);
        headerLayout.setForegroundGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        this.mMoreButton = new Button(this);
        this.mMoreButton.setBackgroundColor(Color.WHITE);
        this.mMoreButton.setText(this.getString(R.string.more));
        this.mMoreButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35.0F);
        this.mMoreButton.setVisibility(View.GONE);

        FrameLayout.LayoutParams buttonLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.mMoreButton.setLayoutParams(buttonLayoutParams);

        headerLayout.addView(this.mMoreButton);

        this.mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConversationActivity.this.getMessages();
            }
        });

        this.mConversationLayoutView = this.findViewById(R.id.conversation_layout);

        this.mMessageBox = (EditText) this.findViewById(R.id.new_message_text);
        this.mMessageBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)) {
                    ConversationActivity.this.mButton.setText(R.string.lol);
                } else {
                    ConversationActivity.this.mButton.setText(R.string.send);
                }

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

                String text = ConversationActivity.this.mMessageBox.getText().toString();

                if (TextUtils.isEmpty(text)) {
                    text = "LOL";
                }

                ConversationActivity.this.showProgress(true);

                messageSender.execute(text);

            }
        });

        File httpCacheDir = new File(this.getCacheDir(), "http");
        long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
        try {
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            this.shortToast("Can't initialize image cache. Expect a slower image fetching.");
        }

        this.mConversationAdapter = new ConversationAdapter(this.mMessages);

        this.mListView = (ListView) this.findViewById(R.id.conversation);

        this.mListView.addHeaderView(headerLayout);

        this.mListView.setAdapter(this.mConversationAdapter);
        this.mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        this.mListView.setStackFromBottom(true);

        this.getMessages();

    }

    @Override
    public void onResume() {
        super.onResume();
        this.mReceiver = this.newReceiver();
        LocalBroadcastManager.getInstance(NerdzMessenger.context).registerReceiver(this.mReceiver, new IntentFilter(GcmIntentService.MESSAGE_EVENT));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(NerdzMessenger.context).unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void onStop() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.updateExitResult();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        this.updateExitResult();
        super.onBackPressed();
    }

    private BroadcastReceiver newReceiver() {

        return new GcmIntentService.LocalMessageReceiver(new GcmIntentService.Operation() {
            @Override
            public void handleMessage(Message message) {
                if(message.thisConversation().getOtherID() == ConversationActivity.this.mThisConversation.getOtherID()) {
                    ConversationActivity.this.append(message);
                    ConversationsListActivity.MessageContainer messageContainer = new ConversationsListActivity.MessageContainer(message);
                    messageContainer.lockRead(true);
                    message = messageContainer;
                }

                Server.getInstance().stashMessage(message);
                ConversationActivity.this.mStashed = true;
            }
        });

    }

    void append(Message message) {
        this.mMessages.add(message);
        this.mConversationAdapter.notifyDataSetChanged();
    }

    private void updateExitResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Keys.OPERATION_RESULT, this.mStashed ? Result.STASH : Result.NONE);
        this.setResult(Activity.RESULT_OK, resultIntent);
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
                this.updateExitResult();
                this.finish();
                return true;

            case R.id.msgs_refresh_button: {
                this.refresh();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /*private void scrollDownList() {
        this.mListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                ConversationActivity.this.mListView.setSelection(ConversationActivity.this.mConversationAdapter.getCount() - 1);
            }
        });
    }*/

    /**
     * Shows the progress bar or not
     */
    private void showProgress(final boolean show) {
        this.setSupportProgressBarIndeterminateVisibility(show);
    }

    private void getMessages() {
        this.getMessages(false);
    }

    private void getMessages(boolean refresh) {

        Log.d(TAG, "getMessages()");

        this.showProgress(true);
        if (this.mMessageFetch != null && this.mMessageFetch.getStatus() == AsyncTask.Status.RUNNING) {
            this.mMessageFetch.cancel(true);
        }
        (this.mMessageFetch = new MessageFetch()).execute(10,refresh ? 1 : 0);

    }

    private void refresh() {
        this.getMessages(true);
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
        public boolean hreffed;

        public ViewHolder(TextView message, TextView date) {
            this.message = message;
            this.date = date;
            this.hreffed = false;
        }
    }

    private class MessageFetch extends AsyncTask<Integer, Void, Pair<List<Message>, Throwable>> {

        Integer mRefresh;

        @Override
        protected Pair<List<Message>, Throwable> doInBackground(Integer... params) {

            Log.d(TAG, "doInBackground()");

            try {


                if(params.length == 2) {
                    this.mRefresh = params[1];
                } else {
                    this.mRefresh = 0;
                }

                if(this.mRefresh != 0) {
                    ConversationActivity.this.mThisConversation.reset();
                }

                ConversationActivity.this.mThisConversation.fetch(params[0]);
                return Pair.create(ConversationActivity.this.mThisConversation.getFetchedMessages(), null);
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
            ConversationActivity.this.mMessages.clear();

            if(this.mRefresh != 0) {
                ConversationActivity.this.mListView.invalidateViews();
                ConversationActivity.this.mSent.clear();
            }

            ConversationActivity.this.mMessages.addAll(result.first);
            ConversationActivity.this.mMessages.addAll(ConversationActivity.this.mSent);

            boolean hasMore = ConversationActivity.this.mThisConversation.hasMore();
            ConversationActivity.this.mMoreButton.setEnabled(hasMore);
            ConversationActivity.this.mMoreButton.setVisibility(hasMore ? View.VISIBLE : View.GONE);

            ConversationActivity.this.mConversationAdapter.notifyDataSetChanged();

            //ConversationActivity.this.scrollDownList();

        }

    }


    private class MessageSender extends AsyncTask<String, Void, Pair<Message, Throwable>> {

        @Override
        protected Pair<Message, Throwable> doInBackground(String... params) {
            try {
                return Pair.create(Server.getInstance().sendMessage(ConversationActivity.this.mThisConversation.getOtherName(), params[0]), null);
            } catch (Throwable t) {
                return Pair.create(null, t);
            }
        }

        @Override
        protected void onPostExecute(Pair<Message, Throwable> result) {

            Throwable t = result.second;

            ConversationActivity.this.showProgress(false);

            if (t != null) {

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

            ConversationActivity.this.mStashed = true;
            Server.getInstance().stashMessage(result.first);
            ConversationActivity.this.mSent.add(result.first);
            ConversationActivity.this.append(result.first);

            //ConversationActivity.this.scrollDownList();

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

        private View newRow() {
            LayoutInflater layoutInflater = this.mActivity.getLayoutInflater();
            View rowView = layoutInflater.inflate(R.layout.conversation_message, null);
            rowView.setTag(new ViewHolder(
                    (TextView) rowView.findViewById(R.id.message_text_view),
                    (TextView) rowView.findViewById(R.id.message_received_date)
            ));
            return rowView;
        }

        private boolean isValidRow(View rowView, int position) {
            return rowView != null && !((ViewHolder) rowView.getTag()).hreffed && position != this.mMessages.size() - 1;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {

            Message element = this.mMessages.get(position);

            ViewHolder tag;

            rowView = this.isValidRow(rowView, position) ? rowView : this.newRow();
            tag = (ViewHolder) rowView.getTag();


            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) tag.message.getLayoutParams();

            int senderId = element.received()
                           ? element.thisConversation().getOtherID()
                           : Server.getInstance().getId();

            if (senderId != Server.getInstance().getId()) {
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

            String text = ConversationActivity.replaceBbcode(element.getContent().replaceAll("\n", "<br>"));
            tag.message.setText(Html.fromHtml(text, new MessageImageLoader(tag.message), null));

            Linkify.addLinks(tag.message, Linkify.ALL);

            tag.message.setMovementMethod(LinkMovementMethod.getInstance());

            if (text.contains("<a href=")) { //this hreffed thing disables recycling of views containing links (fixes weird bugs)
                tag.hreffed = true;
            }

            tag.date.setText(ConversationActivity.formatDate(element.getDate(), this.mActivity));

            return rowView;
        }

        private int dpToPixels(int dp) {
            Resources r = this.mActivity.getResources();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        }

    }

    static String replaceBbcode(String message) {
        return ConversationActivity.replaceBoldItalicsUnderlineDeleted(ConversationActivity.replaceSmallBig(ConversationActivity.replaceUrls(ConversationActivity.replaceImages(message))));
    }

    /**
     * Parses images tags.
     *
     * @param message A message to be parsed
     * @return A string in which all [img]s have been replaced with their URLs
     */
    static String replaceImages(String message) {
        Matcher matcher = Pattern.compile("\\[img\\](.*?)\\[/img\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<img src=\"" + matcher.group(1) + "\" />");
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parses URLs.
     *
     * @param message A message to be parsed
     * @return A string in which all [url]s and [url=...]s have been replaced with their URLs (and description)
     */
    static String replaceUrls(String message) {

        Matcher matcher = Pattern.compile("\\[url=(.*?)\\](.*?)\\[/url\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer result = new StringBuffer();

        String format = "<a href=\"%s\">%s</a>";

        while (matcher.find()) {

            String url = matcher.group(1).trim();

            if (!url.startsWith("http://")) {
                url = "http://" + url;
            }

            matcher.appendReplacement(result, String.format(format, url, matcher.group(2)));
        }

        matcher.appendTail(result);

        message = result.toString();

        matcher = Pattern.compile("\\[url\\](.*?)\\[/url\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        result = new StringBuffer();

        while (matcher.find()) {

            String url = matcher.group(1).trim();

            if (!url.startsWith("http://")) {
                url = "http://" + url;
            }

            matcher.appendReplacement(result, String.format(format, url, url));
        }

        matcher.appendTail(result);

        return result.toString();

    }

    /**
     * Parses [small], [big] tags.
     *
     * @param message
     * @return
     */
    static String replaceSmallBig(String message) {
        Matcher matcher = Pattern.compile("\\[big\\](.*?)\\[/big\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<big>" + matcher.group(1) + "</big>");
        }

        matcher.appendTail(result);

        message = result.toString();

        matcher = Pattern.compile("\\[small\\](.*?)\\[/small\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<small>" + matcher.group(1) + "</small>");
        }

        matcher.appendTail(result);

        return result.toString();
    }

    static String replaceBoldItalicsUnderlineDeleted(String message) {
        Matcher matcher = Pattern.compile("\\[b\\](.*?)\\[/b\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<b>" + matcher.group(1) + "</b>");
        }

        matcher.appendTail(result);

        message = result.toString();

        matcher = Pattern.compile("\\[u\\](.*?)\\[/u\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<u>" + matcher.group(1) + "</u>");
        }

        matcher.appendTail(result);

        message = result.toString();

        matcher = Pattern.compile("\\[del\\](.*?)\\[/del\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<strike>" + matcher.group(1) + "</strike>");
        }

        matcher.appendTail(result);

        message = result.toString();

        matcher = Pattern.compile("\\[i\\](.*?)\\[/i\\]", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(message);
        result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, "<i>" + matcher.group(1) + "</i>");
        }

        matcher.appendTail(result);

        return result.toString();
    }

    class MessageImageLoader implements Html.ImageGetter {

        private final TextView mTextView;

        MessageImageLoader(TextView textView) {
            this.mTextView = textView;
        }

        @Override
        public Drawable getDrawable(String source) {
            LevelListDrawable drawable = new LevelListDrawable();
            Drawable empty = ConversationActivity.this.getResources().getDrawable(R.drawable.ic_menu_refresh);
            drawable.addLevel(0, 0, empty);
            drawable.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
            new LoadImage().execute(source, drawable, this.mTextView);
            return drawable;
        }
    }

    /**
     * Thanks to some guy on stackoverflow for this code. You are the boss, man.
     */
    class LoadImage extends AsyncTask<Object, Void, Bitmap> {

        private LevelListDrawable mDrawable;
        private TextView mTextView;

        @Override
        protected Bitmap doInBackground(Object... params) {
            String source = (String) params[0];
            this.mDrawable = (LevelListDrawable) params[1];
            this.mTextView = (TextView) params[2];
            Log.d(TAG, "doInBackground source " + source);
            try {
                URL imgUrl = new URL(source);
                HttpURLConnection httpConn = (HttpURLConnection) imgUrl.openConnection();
                InputStream is = new URL(source).openStream();
                return BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.d(TAG, "Fetching drawable " + this.mDrawable);
            Log.d(TAG, "Fetching bitmap " + bitmap);
            if (bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(ConversationActivity.this.getResources(), bitmap);
                this.mDrawable.addLevel(1, 1, drawable);
                this.mDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                this.mDrawable.setLevel(1);
            }
            CharSequence text = this.mTextView.getText();
            this.mTextView.setText(text);
        }
    }

}

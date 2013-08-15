
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

public class ConversationsListActivity extends PopupActivity {

    public static final String TAG = "NdzConvListAct";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_conversations_list);

        this.fetchConversations();

    }

    private void fetchConversations() {

        Log.d(TAG, "fetchConversations()");

        new ConversationFetch().execute();

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
            }

            List<Pair<Conversation, Message>> conversations = result.first;

            if (conversations == null) {
                
                TextView textView = (TextView) ConversationsListActivity.this.findViewById(R.id.no_conversations_msg);
                textView.setHeight(73);
                textView.setText(ConversationsListActivity.this.getString(R.string.no_conversations));
                
            } else {}

        }

    }

}

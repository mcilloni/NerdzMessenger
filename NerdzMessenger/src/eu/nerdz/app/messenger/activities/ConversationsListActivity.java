
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

            try {
                List<Conversation> conversations = this.mMessaging.getConversations();
                if (conversations == null)
                    return null;
                List<Pair<Conversation, Message>> newList = new ArrayList<Pair<Conversation, Message>>(conversations.size());
                Pair<List<Pair<Conversation, Message>>, Throwable> result = Pair.create(newList, null);
                for (Conversation conversation : conversations) {
                    Message sample = this.mMessaging.getFirstMessage(conversation);
                }

            } catch (Throwable t) {
                return Pair.create(null, t);
            }

            // } catch (ContentException e) {
            // ConversationsListActivity.this.shortToast("There's something weird in NERDZ Beta. Please, blame Robertof ASAP: "
            // + e.getLocalizedMessage());
            // ConversationsListActivity.this.finish();
            // } catch (IOException e) {
            // ConversationsListActivity.this.shortToast("Network error: " +
            // e.getLocalizedMessage());
            // ConversationsListActivity.this.finish();
            // } catch (HttpException e) {
            // ConversationsListActivity.this.shortToast("HTTP Error: " +
            // e.getLocalizedMessage());
            // }

            // Pair<List<Pair<Conversation, String>>, Throwable> result =
            // Pair<List<Pair<Conversation, String>>, Throwable>.create(new
            // ArrayList<Pair<Conversation, String>>(20),null);

            return null;
        }

        @Override
        protected void onPostExecute(Pair<List<Pair<Conversation, Message>>, Throwable> result) {

        }

    }

}

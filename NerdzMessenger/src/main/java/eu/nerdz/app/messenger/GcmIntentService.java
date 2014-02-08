/*
 * This file is part of NerdzMessenger.
 *
 *     NerdzMessenger is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     NerdzMessenger is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with NerdzMessenger.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     (C) 2013 Marco Cilloni <marco.cilloni@yahoo.com>
 */

package eu.nerdz.app.messenger;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.Date;

import eu.nerdz.api.messages.Conversation;
import eu.nerdz.api.messages.Message;
import eu.nerdz.app.Keys;
import eu.nerdz.app.messenger.activities.ConversationActivity;
import eu.nerdz.app.messenger.activities.ConversationsListActivity;

public class GcmIntentService extends IntentService {

    private final static String TAG = "NerdzMsgGCMService";

    public final static String MESSAGE_EVENT = "NdzMsgMsgArrived";
    public final static int MSG_ID = 142424;

    private static String ellipsize(String string, int length) {

        return string.length() > length ? string.substring(0, length) + '…' : string;

    }

    private NotificationManager mNotificationManager;
    private LocalBroadcastManager mLocalBroadcastManager;

    public GcmIntentService() {
        super(TAG);
    }

    public boolean isActivityOpen() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);

        String name = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();

        return ConversationActivity.class.getCanonicalName().equalsIgnoreCase(name)
               ||
               ConversationsListActivity.class.getCanonicalName().equalsIgnoreCase(name);

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "Received message!");

        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) NerdzMessenger.context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (this.mLocalBroadcastManager == null) {
            this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        }

        Bundle extras = intent.getExtras();

        if (extras == null) {
            return;
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {

            if (!GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                Log.w(TAG, "Unknown message type " + messageType);

            } else {

                this.notifyUser(extras.getString("messageFrom"), Integer.parseInt(extras.getString("messageFromId")), extras.getString("messageBody"));

            }

        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private Message makeMessage(final String from, final int fromId, final String message) {
        return new Message() {

            Date mDate = new Date();

            private Conversation mConversation = new Conversation() {

                boolean mNew = true;
                Date mDate2 = mDate;

                @Override
                public int getOtherID() {
                    return fromId;
                }

                @Override
                public String getOtherName() {
                    return from;
                }

                @Override
                public Date getLastDate() {
                    return this.mDate2;
                }

                @Override
                public boolean hasNewMessages() {
                    return this.mNew;
                }

                @Override
                public void toggleHasNewMessages() {
                    this.mNew = !this.mNew;
                }

                @Override
                public void setHasNewMessages(boolean b) {
                    this.mNew = b;
                }

                @Override
                public void updateConversation(Message message) {
                    this.mDate2 = message.getDate();
                    this.mNew = message.read();
                }
            };

            @Override
            public Conversation thisConversation() {
                return this.mConversation;
            }

            @Override
            public boolean received() {
                return true;
            }

            @Override
            public boolean read() {
                return false;
            }

            @Override
            public String getContent() {
                return message;
            }

            @Override
            public Date getDate() {
                return this.mDate;
            }
        };
    }

    private synchronized void notifyUser(String from, int fromId, String message) {

        from = Html.fromHtml(from).toString();
        message = Html.fromHtml(message).toString();

        if (this.isActivityOpen()) {
            Log.d(TAG, "Activity is open");

            Message message1 = this.makeMessage(from, fromId, message);

            Intent intent = new Intent(GcmIntentService.MESSAGE_EVENT);
            intent.putExtra(GcmIntentService.MESSAGE_EVENT, message1);

            this.mLocalBroadcastManager.sendBroadcast(intent);

            return;
        }

        PowerManager.WakeLock wL = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "notifyWl");

        wL.acquire();

        message = GcmIntentService.ellipsize(message, 60);

        String ticker = from + ": " + message;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int counter = MessagesHolder.append(from, message);

        Log.d(TAG, "" + counter);

        PendingIntent openIntent = null;

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        if (counter > 1) {

            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

            from = counter + " new messages";
            message = "Swipe to see details";

            style.setBigContentTitle(from);
            style.setSummaryText(Server.getInstance().getName());

            for (Pair<String, String> pair : MessagesHolder.get()) {
                style.addLine(Html.fromHtml("<b>" + GcmIntentService.ellipsize(pair.first, 20) + "</b> " + pair.second));
            }

            builder.setStyle(style);

            stackBuilder.addParentStack(ConversationsListActivity.class);
            stackBuilder.addNextIntent(new Intent(this, ConversationsListActivity.class));
            openIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {

            stackBuilder.addParentStack(ConversationActivity.class);

            Intent intent = new Intent(this, ConversationActivity.class);

            intent.putExtra(Keys.FROM, from);
            intent.putExtra(Keys.FROM_ID, fromId);

            stackBuilder.addNextIntent(intent);
            openIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        }

        builder //Sorry Robertof, no enormous oneliners today
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(from)
                .setContentText(message)
                .setContentIntent(openIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setNumber(counter)
                .setPriority(Notification.PRIORITY_HIGH)
                .setTicker(ticker);

        this.mNotificationManager.notify(MSG_ID, builder.build());

        wL.release();

    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static class LocalMessageReceiver extends BroadcastReceiver {

        private final Operation mOperation;

        public LocalMessageReceiver(Operation op) {
            this.mOperation = op;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = (Message) intent.getSerializableExtra(GcmIntentService.MESSAGE_EVENT);

            this.mOperation.handleMessage(message);
        }
    }

    public static interface Operation {
        void handleMessage(Message message);
    }

}

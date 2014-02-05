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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import eu.nerdz.app.messenger.activities.ConversationActivity;

public class GcmIntentService extends IntentService {

    private final static String TAG = "NerdzMsgGCMService";
    public final static int MSG_ID = 142424;

    private static String ellipsize(String string, int length) {

        return string.length() > length ? string.substring(0,length) + '…' : string;

    }

    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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

                this.notifyUser(extras.getString("messageFrom"), extras.getString("messageBody"));

            }

        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private synchronized void notifyUser(String from, String message) {

        PowerManager.WakeLock wL = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "notifyWl");

        wL.acquire();

        from = Html.fromHtml(from).toString();
        message = GcmIntentService.ellipsize(Html.fromHtml(message).toString(), 60);

        String ticker = from + ": " + message;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int counter = MessagesHolder.append(from, message);

        Log.d(TAG, "" + counter);

        if(counter > 1) {

            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

            from = counter + " new messages";
            message = "Swipe to see details";

            style.setBigContentTitle(from);
            style.setSummaryText(MessagesHolder.name());

            for (Pair<String,String> pair : MessagesHolder.get()) {
                style.addLine(Html.fromHtml("<b>" + GcmIntentService.ellipsize(pair.first, 20) + "</b> " + pair.second));
            }

            builder.setStyle(style);

        }

        PendingIntent openIntent = PendingIntent.getActivity(this, 0, new Intent(this, ConversationActivity.class), 0);

        builder //Sorry Robertof, no enormous oneliners today
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(from)
            .setContentText(message)
            .setContentIntent(openIntent)
            .setDefaults(Notification.DEFAULT_ALL)
            .setNumber(counter)
            .setPriority(Notification.PRIORITY_HIGH)
            .setTicker(ticker);

        notificationManager.notify(MSG_ID, builder.build());

        wL.release();

    }

    private void shortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}

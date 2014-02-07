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

package eu.nerdz.app.messenger.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import eu.nerdz.app.messenger.GcmIntentService;
import eu.nerdz.app.messenger.MessagesHolder;

public class NerdzMessengerActivity extends ActionBarActivity {

    protected void longToast(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void longToast(int id) {

        this.longToast(this.getString(id));
    }

    protected void shortToast(int id) {

        this.shortToast(this.getString(id));
    }

    protected void shortToast(String msg) {

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected void unsetNotification() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(GcmIntentService.MSG_ID);

        MessagesHolder.cleanUp();
    }

}

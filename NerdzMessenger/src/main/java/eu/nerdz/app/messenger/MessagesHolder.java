/*
 * This file is part of NerdzApi-java.
 *
 *     NerdzApi-java is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     NerdzApi-java is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with NerdzApi-java.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     (C) 2013 Marco Cilloni <marco.cilloni@yahoo.com>
 */

package eu.nerdz.app.messenger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

import eu.nerdz.app.messenger.activities.NerdzMessengerActivity;

public class MessagesHolder {

    private final static String TAG = "NerdzMsgHolder";

    private final static String sCounterStat = "NdzMsgCounterNotify";
    private final static String sMessagePrefs = "NdzMsg";
    private static int sCounter;
    private static String sUserName;
    private static List<Pair<String,String>> sList;

    static {
        MessagesHolder.sCounter = 0;
    }

    public static synchronized int append(String from, String message) {

        if (MessagesHolder.sCounter == 0) {
            MessagesHolder.sCounter = Prefs.sSharedPreferences.getInt(MessagesHolder.sCounterStat, 0);
        }

        ++MessagesHolder.sCounter;

        String keyFrom = MessagesHolder.sMessagePrefs + MessagesHolder.sCounter + "from";
        String keyMsg = MessagesHolder.sMessagePrefs + MessagesHolder.sCounter + "msg";
        SharedPreferences.Editor editor = Prefs.sSharedPreferences.edit();
        editor.putString(keyFrom, from);
        editor.putString(keyMsg, message);
        editor.putInt(MessagesHolder.sCounterStat, MessagesHolder.sCounter);
        editor.apply();

        if(MessagesHolder.sList != null) {
            MessagesHolder.sList.add(Pair.create(from, message));
        }

        return MessagesHolder.sCounter;

    }

    public static synchronized void cleanUp() {
        SharedPreferences.Editor editor = Prefs.sSharedPreferences.edit();
        String keyFrom, keyMsg;

        int counter = Prefs.sSharedPreferences.getInt(MessagesHolder.sCounterStat,0);

        for(int i = 0; i <= counter; ++i) {
            keyFrom = MessagesHolder.sMessagePrefs + i + "from";
            keyMsg = MessagesHolder.sMessagePrefs + i + "msg";
            editor.remove(keyFrom);
            editor.remove(keyMsg);
        }

        editor.remove(MessagesHolder.sCounterStat);

        MessagesHolder.sCounter = 0;
        MessagesHolder.sList = null;

        editor.apply();
    }

    public static int counter() {

        return (MessagesHolder.sCounter > 0)
               ? MessagesHolder.sCounter
               : (MessagesHolder.sCounter = Prefs.sSharedPreferences.getInt(MessagesHolder.sCounterStat,0));

    }

    public static List<Pair<String,String>> get() {

        if(MessagesHolder.sList != null) {
            return MessagesHolder.sList;
        }

        MessagesHolder.sList = new LinkedList<Pair<String, String>>();

        for(int i = 1; i <= MessagesHolder.counter(); ++i) {
            MessagesHolder.sList.add(Pair.create(
                    Prefs.sSharedPreferences.getString(MessagesHolder.sMessagePrefs + i + "from", "NUFFIN"+ i),
                    Prefs.sSharedPreferences.getString(MessagesHolder.sMessagePrefs + i + "msg", "MUFFIN!" + i)));
        }

        return MessagesHolder.sList;
    }


}

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
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import eu.nerdz.api.BadStatusException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.InvalidManagerException;
import eu.nerdz.api.Nerdz;
import eu.nerdz.api.UserInfo;
import eu.nerdz.api.UserNotFoundException;
import eu.nerdz.api.WrongUserInfoTypeException;
import eu.nerdz.api.messages.Conversation;
import eu.nerdz.api.messages.ConversationHandler;
import eu.nerdz.api.messages.Message;
import eu.nerdz.api.messages.MessageFetcher;
import eu.nerdz.api.messages.Messenger;
import eu.nerdz.app.Keys;

public class Server {

    public static final String TAG = "NdzServer";

    private static Server ourInstance = new Server();
    private SparseArray<Message> mStash;
    private UserInfo mUserInfo;
    private Messenger mMessenger;


    public static Server getInstance() {
        return ourInstance;
    }

    private Server() {
        this.mStash = new SparseArray<Message>();
    }
    
    private AccountManager mAccountManager;

    private synchronized AccountManager getAccountManager() {
        if(this.mAccountManager != null) {
            return this.mAccountManager;
        }

        return (this.mAccountManager = AccountManager.get(NerdzMessenger.context));
    }

    public synchronized List<Conversation> getConversations() throws ClassNotFoundException, WrongUserInfoTypeException, InvalidManagerException, InstantiationException, IllegalAccessException, IOException, HttpException {

        Messenger messenger = this.getMessenger();
        ConversationHandler handler = messenger.getConversationHandler();

        return handler.getConversations();
    }

    public MessageFetcher getFetcher(final String from, final int id) throws ClassNotFoundException, WrongUserInfoTypeException, InvalidManagerException, InstantiationException, IllegalAccessException {
        Conversation thisConversation = new Conversation() {

            private Date mDate = new Date();
            private boolean mNew = true;

            @Override
            public int getOtherID() {
                return id;
            }

            @Override
            public String getOtherName() {
                return from;
            }

            @Override
            public Date getLastDate() {
                return this.mDate;
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
                //do nothing
            }
        };

        return this.getMessenger().getConversationHandler().createFetcher(thisConversation);
    }

    public int getId() {
        UserInfo info = this.getUserData();
        return info.getNerdzID();
    }

    public String getName() {
        UserInfo info = this.getUserData();
        return info.getUsername();
    }
    private synchronized UserInfo getUserData() {

        if(this.mUserInfo != null) {
            return this.mUserInfo;
        }

        AccountManager am = this.getAccountManager();

        Account[] accounts = am.getAccountsByType(NerdzMessenger.context.getString(R.string.account_type));

        if(accounts.length != 1) {
            throw new DieHorriblyError("No account availiable");
        }

        try {
            Nerdz nerdz = Nerdz.getImplementation(Prefs.getImplementationName());
            String userData = this.mAccountManager.getUserData(accounts[0], Keys.NERDZ_INFO);
            this.mUserInfo = nerdz.deserializeFromString(userData);
        } catch (Exception e) {
            throw new DieHorriblyError(e);
        }

        return this.mUserInfo;

    }

    public Message getLastMessage(Conversation conversation) throws ClassNotFoundException, WrongUserInfoTypeException, InvalidManagerException, InstantiationException, IllegalAccessException, IOException, HttpException {
        return this.getMessenger().getConversationHandler().getLastMessage(conversation);
    }

    private synchronized Messenger getMessenger() throws ClassNotFoundException, InvalidManagerException, InstantiationException, IllegalAccessException, WrongUserInfoTypeException {
        if(this.mMessenger != null) {
            return this.mMessenger;
        }

        UserInfo info = this.getUserData();

        return (this.mMessenger = Nerdz.getImplementation(Prefs.getImplementationName()).restoreMessenger(info));
    }

    public void registerGcmUser(final Reaction reaction) {
        new AsyncTask<Void,Void,String>() {

            @Override
            protected String doInBackground(Void... params) {

                try {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(NerdzMessenger.context);

                    String regId = gcm.register(NerdzMessenger.GCM_SENDER_ID);

                    Log.d(TAG, "Received RegId " + regId);

                    Prefs.setGcmRegId(regId);

                    Messenger messenger = Server.this.getMessenger();

                    messenger.registerForPush("GCM", regId);

                } catch (Throwable ex) {
                    Log.w(TAG, ex);
                    return ex.getLocalizedMessage();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String msg) {
                if(msg != null) {
                    reaction.onError(new Exception(msg));
                }
            }

        }.execute();
    }

    public Message sendMessage(String to, String message) throws ClassNotFoundException, WrongUserInfoTypeException, InvalidManagerException, InstantiationException, IllegalAccessException, UserNotFoundException, HttpException, BadStatusException, IOException {
        return this.getMessenger().sendMessage(to, message);
    }

    public synchronized void stashMessage(Message message) {
        this.mStash.put(message.thisConversation().getOtherID(), message);
    }

    public synchronized SparseArray<Message> getStashedMessages() {
        SparseArray<Message> current = this.mStash;
        this.mStash = new SparseArray<Message>();
        return current;
    }

    public void userData(Activity callingActivity, final Reaction reaction) {
        Nerdz nerdzJavaIsDumb = null;
        try {
            nerdzJavaIsDumb = Nerdz.getImplementation(Prefs.getImplementationName());
        } catch (Exception e) {
            reaction.onError(e);
            return;
        }

        AccountManager am = this.getAccountManager();

        final Nerdz nerdz = nerdzJavaIsDumb;

        Account[] accounts = am.getAccountsByType(NerdzMessenger.context.getString(R.string.account_type));

        if (accounts.length != 1) {
            am.addAccount(NerdzMessenger.context.getString(R.string.account_type), null, null, null, callingActivity, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> future) {

                    Log.d(TAG, "Account auth completed");

                    while (true)
                        if (future.isDone()) {
                            break;
                        }

                    try {
                        Bundle result = future.getResult();
                        String userData = result.getString(Keys.NERDZ_INFO);
                        Server.this.mUserInfo = nerdz.deserializeFromString(userData);
                        reaction.onSuccess(Server.this.mUserInfo);
                    } catch (OperationCanceledException e) {
                        Log.d(TAG, "Operation Cancelled.");
                        reaction.onError(new Exception(NerdzMessenger.context.getString(R.string.operation_cancelled)));
                    } catch (IOException e) {
                        Log.e(TAG, "WTF?? IOException:" + e.getLocalizedMessage());
                        reaction.onError(e);
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, "WTF?? AuthenticatorException:" + e.getLocalizedMessage());
                        reaction.onError(e);
                    } catch (WrongUserInfoTypeException e) {
                        Log.e(TAG, NerdzMessenger.context.getString(R.string.api_changed));
                        reaction.onError(e);
                    }
                }
            }, null);
        } else {
            String userData = am.getUserData(accounts[0], Keys.NERDZ_INFO);
            try {
                this.mUserInfo = nerdz.deserializeFromString(userData);
                reaction.onSuccess(this.mUserInfo);
            } catch (WrongUserInfoTypeException e) {
                Log.e(TAG, NerdzMessenger.context.getString(R.string.api_changed));
                reaction.onError(e);
            }
        }

    }

    public static interface Reaction {
        public abstract void onError(Exception message);
        public abstract void onSuccess(UserInfo userData);
    }
}

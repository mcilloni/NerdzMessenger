
package eu.nerdz.app.messenger;

import eu.nerdz.api.BadStatusException;
import eu.nerdz.api.ContentException;
import eu.nerdz.api.HttpException;
import eu.nerdz.api.LoginException;
import eu.nerdz.api.impl.reverse.ReverseLoginData;
import eu.nerdz.api.impl.reverse.messages.ReverseMessenger;
import eu.nerdz.api.messages.Conversation;
import eu.nerdz.api.messages.Message;
import eu.nerdz.api.messages.Messenger;
import java.io.IOException;
import java.util.List;

public class Messaging {

    private static Messaging sOurInstance = new Messaging();
    private Messenger mMessenger = null;
    private UserInfo mUserInfo = new UserInfo() {

        @Override
        public String getToken() throws ContentException {

            return ((ReverseMessenger) Messaging.this.mMessenger).getLoginData().getNerdzU().getValue();

        }

        @Override
        public int getUserID() {

            return Messaging.this.mMessenger.getUserID();

        }

        @Override
        public String getUsername() {

            return Messaging.this.mMessenger.getUsername();

        }

    };

    public static ReverseLoginData doLogin(String userName, String password) throws LoginException, IOException, HttpException, ContentException {

        return new ReverseMessenger(userName, password).getLoginData();

    }

    public static Messaging get() {

        return Messaging.sOurInstance;

    }

    public void deleteConversation(Conversation paramConversation) throws HttpException, BadStatusException, ContentException, IOException {

        this.mMessenger.getConversationHandler().deleteConversation(paramConversation);

    }

    public List<Conversation> getConversations() throws ContentException, IOException, HttpException {

        return this.mMessenger.getConversationHandler().getConversations();

    }

    public List<Message> getMessagesFromConversation(Conversation conversation) throws ContentException, IOException, HttpException {

        return this.mMessenger.getConversationHandler().getMessagesFromConversation(conversation);

    }

    public List<Message> getMessagesFromConversation(Conversation conversation, int start, int howMany) throws ContentException, IOException, HttpException {

        return this.mMessenger.getConversationHandler().getMessagesFromConversation(conversation, start, howMany);

    }

    public UserInfo getUserInfo() {

        return this.mUserInfo;

    }
    
    public void init(String userName, String userID, String nerdzU) throws LoginException, IOException, HttpException {
        
        this.initWithLoginData(new ReverseLoginData(userName, userID, nerdzU));
    }

    public void initWithLoginData(ReverseLoginData loginData) throws LoginException, IOException, HttpException {

        this.mMessenger = new ReverseMessenger(loginData);
    }

    public boolean loggedIn() {

        return this.mMessenger != null;
    }

    public int newMessages() throws ContentException, IOException, HttpException {

        return this.mMessenger.newMessages();
    }
}

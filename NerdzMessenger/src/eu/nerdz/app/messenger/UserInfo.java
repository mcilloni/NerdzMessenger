
package eu.nerdz.app.messenger;

import eu.nerdz.api.ContentException;

public abstract interface UserInfo {

    public abstract String getToken() throws ContentException;

    public abstract int getUserID();

    public abstract String getUsername();
}

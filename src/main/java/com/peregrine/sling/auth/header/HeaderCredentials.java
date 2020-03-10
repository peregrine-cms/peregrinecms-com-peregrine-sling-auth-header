package com.peregrine.sling.auth.header;

import javax.jcr.Credentials;

public class HeaderCredentials implements Credentials
{
    private String userId;

    public HeaderCredentials(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }
}

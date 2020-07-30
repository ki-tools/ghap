package io.ghap.auth.authorize;

import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.XMLGroupDatabase;

import java.util.Collection;

public class GhapGroupDatabase extends XMLGroupDatabase {
    @Override
    public Group[] groups() throws WikiSecurityException
    {
        Group[] groups = new Group[0];
        Collection<GhapGroup> ghapGroups =  UserManagementClient.getInstance().getGroups();

        return groups;
    }
}

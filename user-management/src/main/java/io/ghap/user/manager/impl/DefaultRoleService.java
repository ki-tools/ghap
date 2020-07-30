package io.ghap.user.manager.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.ldap.LdapUtils;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;

import javax.ws.rs.Path;

@Path("role")
@Singleton
public class DefaultRoleService extends DefaultGroupService {

    private volatile Dn defaultDn;

    @Inject GroupDao groupDao;

    @Override
    protected Dn getDefaultParent(String parentDn) throws LdapException {

        if(defaultDn == null) {
            defaultDn = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("ou=roles");
            Group group = groupDao.find(defaultDn.toString());
            if(group == null){
                defaultDn = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("cn=roles");
            }
        }

        return ("default".equalsIgnoreCase(parentDn)) ? defaultDn : new Dn(parentDn);
    }

}

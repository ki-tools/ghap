package io.ghap.web.tests.user;

import com.netflix.governator.annotations.AutoBindSingleton;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.model.Domain;
import org.apache.directory.api.ldap.model.exception.LdapException;

@AutoBindSingleton(DomainDao.class)
public class TestDomainDao implements DomainDao {
    private static final Domain domain = new Domain("dc=ad,dc=loc");
    static {
        domain.setMaxPwdAge(999);
        domain.setMinPwdLength(8);
    }
    @Override
    public Domain find(String dn) throws LdapException {
        return domain;
    }

    @Override
    public Domain find() throws LdapException {
        return domain;
    }
}

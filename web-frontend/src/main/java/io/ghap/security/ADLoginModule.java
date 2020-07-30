package io.ghap.security;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

public class ADLoginModule implements LoginModule
{
  private String username;
  private Subject subject;
  private Principal userPrincipal;
  private Principal groupPrincipal;
  private CallbackHandler callbackHandler;
  private List<String> groups = new ArrayList<String>();
  
  @Override
  public void initialize(Subject subject, 
                         CallbackHandler callbackHandler,
                         Map<String, ?> sharedState, 
                         Map<String, ?> options)
  {
    this.subject = subject;
    this.callbackHandler = callbackHandler;
  }

  @Override
  public boolean login() throws LoginException
  {
    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("login");
    callbacks[1] = new PasswordCallback("password", false);
    LdapConnection lconn = null;
    try
    {
      String username = null;
      String password = null;
      if(callbackHandler != null) 
      {
        callbackHandler.handle(callbacks);
        username = ((NameCallback)callbacks[0]).getName();
        password = String.valueOf(((PasswordCallback)callbacks[1]).getPassword());
      }
      else
      {
        username = "snagy";
        password = "";
      }
      
      this.username = username;
      
      lconn = new LdapNetworkConnection("samba-pdc.ghap.io");
      BindRequest bReq = new BindRequestImpl();
      bReq.setSimple(true);
      bReq.setName(username+"@dev.ghap.io");
      bReq.setCredentials(password);
      lconn.bind(bReq);
      String filter = String.format("(&(objectClass=user)(sAMAccountName=%s))", username);
      EntryCursor cursor = lconn.search("cn=Users,dc=dev,dc=ghap,dc=io", filter, SearchScope.ONELEVEL, "*");
      
      if(cursor.next())
      {
        Entry entry = cursor.get();
        Attribute attr = entry.get("memberOf");
        Iterator<Value<?>> it = attr.iterator();
        while(it.hasNext())
        {
          Value<?> value = it.next();
          String query = value.getString();
          filter = String.format("(dn=%s)", query);
          EntryCursor groupCursor = lconn.search("cn=Users,dc=dev,dc=ghap,dc=io", filter, SearchScope.ONELEVEL, "*");
          if(groupCursor.next()) {
            Entry groupEntry = groupCursor.get();
            Attribute groupName = groupEntry.get("sAMAccountName");
            groups.add(groupName.getString());
          }
          groupCursor.close();
        }
        cursor.close();
        return true;
      }
      cursor.close();
    }
    catch(IOException ioe) 
    {
      throw new LoginException(ioe.getMessage());
    }
    catch(UnsupportedCallbackException uce)
    {
      throw new LoginException(uce.getMessage());
    }
    catch(LdapException le)
    {
      throw new LoginException(le.getMessage());
    }
    catch(CursorException ce)
    {
      throw new LoginException(ce.getMessage());
    }
    finally
    {
      if(lconn != null)
      {
        try { lconn.unBind(); } catch(Exception e) {}
        try { lconn.close(); } catch(Exception e) {}
      }
    }
    return false;
  }

  @Override
  public boolean commit() throws LoginException
  {
    userPrincipal = new LdapUserPrincipal(username);
    subject.getPrincipals().add(userPrincipal);

    if (groups != null && groups.size() > 0) {
      for (String groupName : groups) {
        groupPrincipal = new LdapGroupPrincipal(groupName);
        subject.getPrincipals().add(groupPrincipal);
      }
    }
    return true;
  }

  @Override
  public boolean abort() throws LoginException
  {
    return false;
  }

  @Override
  public boolean logout() throws LoginException
  {
    subject.getPrincipals().remove(userPrincipal);
    subject.getPrincipals().remove(groupPrincipal);
    return false;
  }
  
  public static void main(String[] args) {
    try {
      new ADLoginModule().login();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}

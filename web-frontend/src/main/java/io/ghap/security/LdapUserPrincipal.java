package io.ghap.security;

import java.security.Principal;

public class LdapUserPrincipal implements Principal
{
  private String name;

  public LdapUserPrincipal(String name)
  {
    super();
    this.name = name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  @Override
  public String getName()
  {
    return name;
  }
}

package io.ghap.user.manager;

public class ServiceStatus {
    private Object ldap;
    private Object oauth;

    public Object getOauth() {
        return oauth;
    }

    public void setOauth(Object oauth) {
        this.oauth = oauth;
    }

    public Object getLdap() {
        return ldap;
    }

    public void setLdap(Object ldap) {
        this.ldap = ldap;
    }

}

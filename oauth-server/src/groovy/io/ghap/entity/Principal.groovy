package io.ghap.entity

/**
 */
class Principal {

    String name;
    String email;
    String password;
    String dn;
    boolean passwordExpired;
    Set<String> roles;
    Set<String> groups;
    boolean adminPrincipal
    Map<String, String> attributes;
    boolean firstTimeLogon
}

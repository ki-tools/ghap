package io.ghap.ldap;

/**
 *
 */
public class Const {
    // some useful constants from lmaccess.h
    public static final int UF_ACCOUNTDISABLE = 0x0002;
    public static final int UF_PASSWD_NOTREQD = 0x0020;
    public static final int UF_PASSWD_CANT_CHANGE = 0x0040;
    public static final int UF_NORMAL_ACCOUNT = 0x0200;
    public static final int UF_DONT_EXPIRE_PASSWD = 0x10000;
    public static final int UF_PASSWORD_EXPIRED = 0x800000;

    public static final String ROLES_CN_NAME = "Roles";
}

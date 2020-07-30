package io.ghap.ldap

import javax.naming.directory.Attribute

/**
 */
class LdapUtils {

    private final static int ONE_HUNDRED_NANOSECOND = 10000000;
    private final static long DIFF_NET_JAVA_FOR_DATE_AND_TIMES = 11644473600000L + 24 * 60 * 60 * 1000;
    public static final int UF_DONT_EXPIRE_PASSWD = 0x10000;
    //NET date is number of 100-nanosecond intervals since January 1, 1601

    public static Integer getPasswordMaxAgeSeconds(Attribute entry) {
        if (entry != null) {
            def maxPwdAgeStr = entry.get();
            if (maxPwdAgeStr != null) {
                try {
                    return (Math.abs(Long.valueOf(maxPwdAgeStr)) / ONE_HUNDRED_NANOSECOND).intValue();
                } catch (NumberFormatException e) {
                    //do nothing
                }
            }
        }
        return null;
    }

    public static boolean isUserMustResetPwd(Integer accountControl) {
        if (accountControl == null) {
            return false;
        }
        return (8388608 & accountControl) == 8388608;
    }

    public static boolean isPasswordNeverExpires(Integer accountControl) {
        if (accountControl == null) {
            return false;
        }
        return (UF_DONT_EXPIRE_PASSWD & accountControl) == UF_DONT_EXPIRE_PASSWD;
    }


    public static final Date fromLdapDate(String dateStr) {
        long adDateTime = Long.parseLong(dateStr);
        long milliseconds = (adDateTime / 10000) - DIFF_NET_JAVA_FOR_DATE_AND_TIMES;
        Date pwdLastSetDate = new Date(milliseconds);
        return pwdLastSetDate;
    }
}

package io.ghap.ldap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class LdapUtils {
    private final static long DIFF_NET_JAVA_FOR_DATE_AND_TIMES = 11644473600000L + 24 * 60 * 60 * 1000; //NET date is number of 100-nanosecond intervals since January 1, 1601

    private final static int[] OBJECT_GUID_FORMAT = new int[]{3,2,1,0,5,4,7,6,8,9,10,11,12,13,14,15};

    public static final Dn toDn(String realm) throws LdapInvalidDnException {
        String dnStr = Stream.of(realm.split("\\."))
                .map(it -> "dc=" + it)
                .collect( Collectors.joining(",") );
        return new Dn(dnStr);
    }

    public static final Date fromLdapDate(String dateStr){
        long adDateTime = Long.parseLong(dateStr);
        long milliseconds = (adDateTime / 10000) - DIFF_NET_JAVA_FOR_DATE_AND_TIMES;
        Date pwdLastSetDate = new Date(milliseconds);
        return pwdLastSetDate;
    }

    public static final long toLdapDate(Date date) {
        return (date.getTime() + DIFF_NET_JAVA_FOR_DATE_AND_TIMES) * 10000;
    }

    public static final String binarySidToStringSid( byte[] SID ) {

        String strSID = "";

        //convert the SID into string format

        long version;
        long authority;
        long count;
        long rid;

        strSID = "S";
        version = SID[0];
        strSID = strSID + "-" + Long.toString(version);
        authority = SID[4];

        for (int i = 0;i<4;i++) {
            authority <<= 8;
            authority += SID[4+i] & 0xFF;
        }

        strSID = strSID + "-" + Long.toString(authority);
        count = SID[2];
        count <<= 8;
        count += SID[1] & 0xFF;

        for (int j=0;j<count;j++) {

            rid = SID[11 + (j*4)] & 0xFF;

            for (int k=1;k<4;k++) {

                rid <<= 8;

                rid += SID[11-k + (j*4)] & 0xFF;

            }

            strSID = strSID + "-" + Long.toString(rid);

        }

        return strSID;

    }

    public static final String convertToDashedString(byte[] objectGUID) {
        StringBuilder displayStr = new StringBuilder();

        displayStr.append(prefixZeros((int) objectGUID[3] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[2] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[1] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[0] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[5] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[4] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[7] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[6] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[8] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[9] & 0xFF));
        displayStr.append("-");
        displayStr.append(prefixZeros((int) objectGUID[10] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[11] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[12] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[13] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[14] & 0xFF));
        displayStr.append(prefixZeros((int) objectGUID[15] & 0xFF));

        return displayStr.toString();
    }

    private static final String prefixZeros(int value) {
        if (value <= 0xF) {
            StringBuilder sb = new StringBuilder("0");
            sb.append(Integer.toHexString(value));

            return sb.toString();

        } else {
            return Integer.toHexString(value);
        }
    }

    public static String convertToBindingString(byte[] objectGUID) {
        StringBuilder displayStr = new StringBuilder();

        displayStr.append("<GUID=");
        displayStr.append(convertToDashedString(objectGUID));
        displayStr.append(">");

        return displayStr.toString();
    }

    public static String convertToByteString(byte[] objectGUID) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < objectGUID.length; i++) {
            String transformed = prefixZeros((int) objectGUID[i] & 0xFF);
            result.append("\\");
            result.append(transformed);
        }

        return result.toString();
    }

    public static String convertToByteString(String objectGUID) {
        byte[] bytes = new byte[16];
        String str = objectGUID.replaceAll("-","");
        for(int i=0;i < 16;i++){
            int pos = i*2;
            String ch = str.substring(pos,pos+2);
            bytes[OBJECT_GUID_FORMAT[i]] = (byte)(int)Integer.valueOf(ch, 16);
        }
        return convertToByteString(bytes);
    }

    /**
     * Converts a String SID to its binary representation, according to the
     * algorithm described <a
     * href="http://blogs.msdn.com/oldnewthing/archive/2004/03/15/89753.aspx"
     * >here</a>.
     *
     * @param string SID in readable format
     * @return Binary version of the given sid
     * @see LdapUtils#binarySidToStringSid(byte[])
     * @since 1.3.1
     */
    public static byte[] convertStringSidToBinary(String string) {
        String[] parts = string.split("-");
        byte sidRevision = (byte) Integer.parseInt(parts[1]);
        int subAuthCount = parts.length - 3;

        byte[] sid = new byte[] {sidRevision, (byte) subAuthCount};
        sid = ArrayUtils.addAll(sid, numberToBytes(parts[2], 6, true));
        for (int i = 0; i < subAuthCount; i++) {
            sid = ArrayUtils.addAll(sid, numberToBytes(parts[3 + i], 4, false));
        }
        return sid;
    }

    /**
     * Converts the given number to a binary representation of the specified
     * length and "endian-ness".
     *
     * @param number String with number to convert
     * @param length How long the resulting binary array should be
     * @param bigEndian <code>true</code> if big endian (5=0005), or
     * <code>false</code> if little endian (5=5000)
     * @return byte array containing the binary result in the given order
     */
    static byte[] numberToBytes(String number, int length, boolean bigEndian) {
        BigInteger bi = new BigInteger(number);
        byte[] bytes = bi.toByteArray();
        int remaining = length - bytes.length;
        if (remaining < 0) {
            bytes = ArrayUtils.subarray(bytes, -remaining, bytes.length);
        } else {
            byte[] fill = new byte[remaining];
            bytes = ArrayUtils.addAll(fill, bytes);
        }
        if (!bigEndian) {
            ArrayUtils.reverse(bytes);
        }
        return bytes;
    }
}

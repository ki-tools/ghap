package io.ghap.oauth

class AccessToken {

    String authenticationKey
    byte[] authentication

    String username
    String clientId

    String value
    String tokenType

    Date expiration
    Date lastUsed

    String idToken;
    String nonce;
    String subject;

    static hasOne = [refreshToken: String]
    static hasMany = [scope: String]

    static constraints = {
        username nullable: true
        clientId nullable: false, blank: false
        value nullable: false, blank: false, unique: true
        tokenType nullable: false, blank: false
        expiration nullable: false
        scope nullable: false
        refreshToken nullable: true
        authenticationKey nullable: false, blank: false, unique: true
        authentication nullable: false, minSize: 1, maxSize: 1024 * 4
        lastUsed nullable: true
        idToken nullable: true, blank: false, maxSize: 1024
        nonce nullable: true, blank: false, maxSize: 50
        subject nullable: true, blank: false, maxSize: 512
    }

    static mapping = {
        version false
        scope lazy: false, cascade: 'all'
    }

    def beforeInsert() {
        lastUsed = new Date();
        log.info("before insert token. scope = ${scope}")
    }


    @Override
    public String toString() {
        return "AccessToken{" +
                "id=" + id +
                ", authenticationKey='" + authenticationKey + '\'' +
                ", username='" + username + '\'' +
                ", clientId='" + clientId + '\'' +
                ", value='" + value + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiration=" + expiration +
                ", lastUsed=" + lastUsed +
                ", idToken='" + idToken + '\'' +
                ", nonce='" + nonce + '\'' +
                ", subject='" + subject + '\'' +
                ", version=" + version +
                ", scope=" + scope +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }
}

package io.ghap.user.dao;

import org.apache.commons.lang.RandomStringUtils;

import javax.inject.Singleton;
import java.security.SecureRandom;

@Singleton
public class PasswordGenerator {
    public String generate(){
        String password = RandomStringUtils.random(6, 0, 0, true, true, null, new SecureRandom());
        password += RandomStringUtils.random(1,"0123456789");
        password += RandomStringUtils.random(1,"abcdefghijklmnopqrstuvwxyz");
        password += RandomStringUtils.random(1,"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        password += RandomStringUtils.random(1,"!@#$%^&*(){}[]`~");
        return password;
    }
}

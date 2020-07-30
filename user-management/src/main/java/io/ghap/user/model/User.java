package io.ghap.user.model;

import io.ghap.user.model.validation.OnCreate;
import io.ghap.user.model.validation.OnResetPassword;
import io.ghap.user.model.validation.OnUpdate;
import io.ghap.user.model.validation.validators.PasswordLength;
import io.ghap.user.model.validation.validators.PasswordPattern;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

import static io.ghap.ldap.Const.UF_DONT_EXPIRE_PASSWD;
import static io.ghap.ldap.Const.UF_PASSWORD_EXPIRED;
import static java.util.Objects.requireNonNull;

//import org.apache.bval.constraints.NotEmpty;


public class User extends AbstractModel {

    @JsonIgnore private Integer userAccountControl;


    /*
    @PasswordPattern.Several(count = 3, groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class}, value = {
            @PasswordPattern(regexp = "^.*\\d.*$"),
            @PasswordPattern(regexp = "^.*[A-Z].*$"),
            @PasswordPattern(regexp = "^.*[a-z].*$"),
            @PasswordPattern(regexp = "^.*[!@#$%^&*(){}\\[\\]`~].*$")
    })
    */
    @JsonIgnore
    @PasswordPattern.List({
            @PasswordPattern(regexp = "^.*\\d.*$"                   , message = "Digits"          , info = "0-9"              ,groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class}),
            @PasswordPattern(regexp = "^.*[A-Z].*$"                 , message = "Uppercase"       , info = "A-Z"              ,groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class}),
            @PasswordPattern(regexp = "^.*[a-z].*$"                 , message = "Lowercase"       , info = "a-z"              ,groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class}),
            @PasswordPattern(regexp = "^.*[!@#$%^&*(){}\\[\\]`~].*$", message = "NonAlphanumeric" , info = "!@#$%^&*(){}[]`~" ,groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class})
    })
    @PasswordLength(groups = {OnCreate.class, OnUpdate.class, OnResetPassword.class})
    @NotNull(groups = {OnCreate.class, OnResetPassword.class})
    private String password;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String fullName;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private Date passwordExpiresDate;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private Date pwdLastSet;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private Date badPasswordTime;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private Integer badPwdCount;

    @NotNull(groups = OnCreate.class)
    @NotEmpty(groups = {OnCreate.class, OnUpdate.class})
    @Length(groups = {OnCreate.class, OnUpdate.class}, max = 20)
    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String name;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String firstName;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String lastName;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    private String email;
    private Boolean locked;

    private boolean resetPassword;

    public User() {
        super("user");
    }

    public User(String dn) {
        super("user");
        setDn(requireNonNull(dn, "User DN should be specified"));
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getUserAccountControl() {
        return userAccountControl;
    }

    public void setUserAccountControl(int userAccountControl) {
        this.userAccountControl = userAccountControl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public Boolean isDisabled(){
        return (userAccountControl == null) ? null : (userAccountControl & (1 << 1)) != 0;
    }

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public Boolean isLocked(){
        return locked;
    }

    public Date getPwdLastSet() {
        return pwdLastSet;
    }

    public void setPwdLastSet(Date pwdLastSet) {
        this.pwdLastSet = pwdLastSet;
    }

    public boolean isResetPassword() {
        return resetPassword;
    }

    public void setResetPassword(boolean resetPassword) {
        this.resetPassword = resetPassword;
    }

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public Boolean isPasswordNeverExpire() {
        return (userAccountControl == null) ? null : (userAccountControl & UF_DONT_EXPIRE_PASSWD) == UF_DONT_EXPIRE_PASSWD;
    }

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public Boolean isPasswordExpiredFlag() {
        return (userAccountControl == null) ? null : (userAccountControl & UF_PASSWORD_EXPIRED) == UF_PASSWORD_EXPIRED;
    }

    public Date getBadPasswordTime() {
        return badPasswordTime;
    }

    public void setBadPasswordTime(Date badPasswordTime) {
        this.badPasswordTime = badPasswordTime;
    }

    public Integer getBadPwdCount() {
        return badPwdCount;
    }

    public void setBadPwdCount(Integer badPwdCount) {
        this.badPwdCount = badPwdCount;
    }

    public Date getPasswordExpiresDate() {
        return passwordExpiresDate;
    }

    public void setDomainRelatedProperties(Domain domain){
        requireNonNull(domain, "Domain argument is null");
        int maxPwdAge = requireNonNull( domain.getMaxPwdAge(), "\""+domain.getDn()+"\" domain attribute value \"maxPwdAge\" is not defined");

        calcPasswordExpiresDate(maxPwdAge);
        if(domain.getLockoutThreshold() != null) {
            setLocked(domain.getLockoutThreshold());
        }
    }

    private Date calcPasswordExpiresDate(int maxPwdAgeDays) {
        Date pwdLastSetDate = getPwdLastSet();
        if(pwdLastSetDate != null) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(pwdLastSetDate);
            cal.add(Calendar.DATE, maxPwdAgeDays);

            this.passwordExpiresDate = cal.getTime();
        }
        return this.passwordExpiresDate;
    }

    private void setLocked(int lockoutThreshold){
        if(lockoutThreshold > 0) {
            this.locked = (this.badPwdCount >= lockoutThreshold);
            // TODO investigate AD behaviour. May be we need to check "lockoutTime" attribute instead
        }
    }

    @Override
    public String toString() {
        return "User [dn=" + getDn() + ", firstName="
                + firstName + ", lastName=" + lastName + "]";
    }

    public static int compareByFullName(User a, User b) {
        return a.getFullName().compareTo(b.getFullName());
    }


}

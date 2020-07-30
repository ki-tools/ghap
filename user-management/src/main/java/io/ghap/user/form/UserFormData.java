package io.ghap.user.form;

import io.ghap.user.model.User;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFormData {

    private String dn;
    private String firstName;
    private String lastName;
    private String email;
    private String name;
    private String parentDn;
    private String password;
    private String currentPassword;
    private Boolean disabled;
    private Boolean notifyByEmail;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentDn() {
        return parentDn;
    }

    public void setParentDn(String parentDn) {
        this.parentDn = parentDn;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public boolean isNotifyByEmail() {
        return Boolean.TRUE.equals(notifyByEmail);
    }

    public void setNotifyByEmail(Boolean notifyByEmail) {
        this.notifyByEmail = notifyByEmail;
    }

    public User toUser(User user){
        if(user == null)
            user = new User();

        if(dn != null)
            user.setDn(dn);
        if(email != null)
            user.setEmail(email);
        if(firstName != null)
            user.setFirstName(firstName);
        if(lastName != null)
            user.setLastName(lastName);
        if(name != null)
            user.setName(name);
        if(password != null)
            user.setPassword(password);

        return user;
    }
    public User toUser(){
        return toUser(null);
    }
}

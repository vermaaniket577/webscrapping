package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: LoginRequest.class */
public class LoginRequest {
    private String userName;
    private String password;

    @Generated
    public LoginRequest() {
    }

    @Generated
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    @Generated
    public void setPassword(final String password) {
        this.password = password;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LoginRequest)) {
            return false;
        }
        LoginRequest other = (LoginRequest) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$userName = getUserName();
        Object other$userName = other.getUserName();
        if (this$userName == null) {
            if (other$userName != null) {
                return false;
            }
        } else if (!this$userName.equals(other$userName)) {
            return false;
        }
        Object this$password = getPassword();
        Object other$password = other.getPassword();
        return this$password == null ? other$password == null : this$password.equals(other$password);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof LoginRequest;
    }

    @Generated
    public int hashCode() {
        Object $userName = getUserName();
        int result = (1 * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        return (result * 59) + ($password == null ? 43 : $password.hashCode());
    }

    @Generated
    public String toString() {
        return "LoginRequest(userName=" + getUserName() + ", password=" + getPassword() + ")";
    }

    @Generated
    public String getUserName() {
        return this.userName;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }
}

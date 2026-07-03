package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: CookieForSession.class */
public class CookieForSession {
    private String cookies;
    private String cookieType;
    private String mobile;
    private String password;

    @Generated
    public void setCookies(final String cookies) {
        this.cookies = cookies;
    }

    @Generated
    public void setCookieType(final String cookieType) {
        this.cookieType = cookieType;
    }

    @Generated
    public void setMobile(final String mobile) {
        this.mobile = mobile;
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
        if (!(o instanceof CookieForSession)) {
            return false;
        }
        CookieForSession other = (CookieForSession) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$cookies = getCookies();
        Object other$cookies = other.getCookies();
        if (this$cookies == null) {
            if (other$cookies != null) {
                return false;
            }
        } else if (!this$cookies.equals(other$cookies)) {
            return false;
        }
        Object this$cookieType = getCookieType();
        Object other$cookieType = other.getCookieType();
        if (this$cookieType == null) {
            if (other$cookieType != null) {
                return false;
            }
        } else if (!this$cookieType.equals(other$cookieType)) {
            return false;
        }
        Object this$mobile = getMobile();
        Object other$mobile = other.getMobile();
        if (this$mobile == null) {
            if (other$mobile != null) {
                return false;
            }
        } else if (!this$mobile.equals(other$mobile)) {
            return false;
        }
        Object this$password = getPassword();
        Object other$password = other.getPassword();
        return this$password == null ? other$password == null : this$password.equals(other$password);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof CookieForSession;
    }

    @Generated
    public int hashCode() {
        Object $cookies = getCookies();
        int result = (1 * 59) + ($cookies == null ? 43 : $cookies.hashCode());
        Object $cookieType = getCookieType();
        int result2 = (result * 59) + ($cookieType == null ? 43 : $cookieType.hashCode());
        Object $mobile = getMobile();
        int result3 = (result2 * 59) + ($mobile == null ? 43 : $mobile.hashCode());
        Object $password = getPassword();
        return (result3 * 59) + ($password == null ? 43 : $password.hashCode());
    }

    @Generated
    public String toString() {
        return "CookieForSession(cookies=" + getCookies() + ", cookieType=" + getCookieType() + ", mobile=" + getMobile() + ", password=" + getPassword() + ")";
    }

    @Generated
    public CookieForSession(final String cookies, final String cookieType, final String mobile, final String password) {
        this.cookies = cookies;
        this.cookieType = cookieType;
        this.mobile = mobile;
        this.password = password;
    }

    @Generated
    public String getCookies() {
        return this.cookies;
    }

    @Generated
    public String getCookieType() {
        return this.cookieType;
    }

    @Generated
    public String getMobile() {
        return this.mobile;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }
}

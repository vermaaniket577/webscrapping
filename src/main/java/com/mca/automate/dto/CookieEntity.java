package com.mca.automate.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Generated;

/* JADX INFO: loaded from: CookieEntity.class */
@Entity
public class CookieEntity {

    @Id
    private String emailId;

    @Column(name = "COOKIES", length = 2000)
    private String cookies;
    private String cookieType;
    private String password;
    private String mobileNo;
    private String deviceId;
    private long updatedAt;

    @Generated
    public CookieEntity() {
    }

    @Generated
    public void setEmailId(final String emailId) {
        this.emailId = emailId;
    }

    @Generated
    public void setCookies(final String cookies) {
        this.cookies = cookies;
    }

    @Generated
    public void setCookieType(final String cookieType) {
        this.cookieType = cookieType;
    }

    @Generated
    public void setPassword(final String password) {
        this.password = password;
    }

    @Generated
    public void setMobileNo(final String mobileNo) {
        this.mobileNo = mobileNo;
    }

    @Generated
    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    @Generated
    public void setUpdatedAt(final long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CookieEntity)) {
            return false;
        }
        CookieEntity other = (CookieEntity) o;
        if (!other.canEqual(this) || getUpdatedAt() != other.getUpdatedAt()) {
            return false;
        }
        Object this$emailId = getEmailId();
        Object other$emailId = other.getEmailId();
        if (this$emailId == null) {
            if (other$emailId != null) {
                return false;
            }
        } else if (!this$emailId.equals(other$emailId)) {
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
        Object this$password = getPassword();
        Object other$password = other.getPassword();
        if (this$password == null) {
            if (other$password != null) {
                return false;
            }
        } else if (!this$password.equals(other$password)) {
            return false;
        }
        Object this$mobileNo = getMobileNo();
        Object other$mobileNo = other.getMobileNo();
        if (this$mobileNo == null) {
            if (other$mobileNo != null) {
                return false;
            }
        } else if (!this$mobileNo.equals(other$mobileNo)) {
            return false;
        }
        Object this$deviceId = getDeviceId();
        Object other$deviceId = other.getDeviceId();
        return this$deviceId == null ? other$deviceId == null : this$deviceId.equals(other$deviceId);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof CookieEntity;
    }

    @Generated
    public int hashCode() {
        long $updatedAt = getUpdatedAt();
        int result = (1 * 59) + ((int) (($updatedAt >>> 32) ^ $updatedAt));
        Object $emailId = getEmailId();
        int result2 = (result * 59) + ($emailId == null ? 43 : $emailId.hashCode());
        Object $cookies = getCookies();
        int result3 = (result2 * 59) + ($cookies == null ? 43 : $cookies.hashCode());
        Object $cookieType = getCookieType();
        int result4 = (result3 * 59) + ($cookieType == null ? 43 : $cookieType.hashCode());
        Object $password = getPassword();
        int result5 = (result4 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $mobileNo = getMobileNo();
        int result6 = (result5 * 59) + ($mobileNo == null ? 43 : $mobileNo.hashCode());
        Object $deviceId = getDeviceId();
        return (result6 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "CookieEntity(emailId=" + getEmailId() + ", cookies=" + getCookies() + ", cookieType=" + getCookieType() + ", password=" + getPassword() + ", mobileNo=" + getMobileNo() + ", deviceId=" + getDeviceId() + ", updatedAt=" + getUpdatedAt() + ")";
    }

    @Generated
    public String getEmailId() {
        return this.emailId;
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
    public String getPassword() {
        return this.password;
    }

    @Generated
    public String getMobileNo() {
        return this.mobileNo;
    }

    @Generated
    public String getDeviceId() {
        return this.deviceId;
    }

    @Generated
    public long getUpdatedAt() {
        return this.updatedAt;
    }
}

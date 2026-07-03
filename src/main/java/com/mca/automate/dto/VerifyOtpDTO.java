package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: VerifyOtpDTO.class */
public class VerifyOtpDTO {
    private String email;
    private String password;
    private String mobile;
    private String otp;
    private String cookie;
    private String deviceId;
    private String sblUserId;

    public VerifyOtpDTO() {
    }

    @Generated
    public void setEmail(final String email) {
        this.email = email;
    }

    @Generated
    public void setPassword(final String password) {
        this.password = password;
    }

    @Generated
    public void setMobile(final String mobile) {
        this.mobile = mobile;
    }

    @Generated
    public void setOtp(final String otp) {
        this.otp = otp;
    }

    @Generated
    public void setCookie(final String cookie) {
        this.cookie = cookie;
    }

    @Generated
    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    @Generated
    public void setSblUserId(final String sblUserId) {
        this.sblUserId = sblUserId;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof VerifyOtpDTO)) {
            return false;
        }
        VerifyOtpDTO other = (VerifyOtpDTO) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$email = getEmail();
        Object other$email = other.getEmail();
        if (this$email == null) {
            if (other$email != null) {
                return false;
            }
        } else if (!this$email.equals(other$email)) {
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
        Object this$mobile = getMobile();
        Object other$mobile = other.getMobile();
        if (this$mobile == null) {
            if (other$mobile != null) {
                return false;
            }
        } else if (!this$mobile.equals(other$mobile)) {
            return false;
        }
        Object this$otp = getOtp();
        Object other$otp = other.getOtp();
        if (this$otp == null) {
            if (other$otp != null) {
                return false;
            }
        } else if (!this$otp.equals(other$otp)) {
            return false;
        }
        Object this$cookie = getCookie();
        Object other$cookie = other.getCookie();
        if (this$cookie == null) {
            if (other$cookie != null) {
                return false;
            }
        } else if (!this$cookie.equals(other$cookie)) {
            return false;
        }
        Object this$deviceId = getDeviceId();
        Object other$deviceId = other.getDeviceId();
        return this$deviceId == null ? other$deviceId == null : this$deviceId.equals(other$deviceId);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof VerifyOtpDTO;
    }

    @Generated
    public int hashCode() {
        Object $email = getEmail();
        int result = (1 * 59) + ($email == null ? 43 : $email.hashCode());
        Object $password = getPassword();
        int result2 = (result * 59) + ($password == null ? 43 : $password.hashCode());
        Object $mobile = getMobile();
        int result3 = (result2 * 59) + ($mobile == null ? 43 : $mobile.hashCode());
        Object $otp = getOtp();
        int result4 = (result3 * 59) + ($otp == null ? 43 : $otp.hashCode());
        Object $cookie = getCookie();
        int result5 = (result4 * 59) + ($cookie == null ? 43 : $cookie.hashCode());
        Object $deviceId = getDeviceId();
        return (result5 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "VerifyOtpDTO(email=" + getEmail() + ", password=" + getPassword() + ", mobile=" + getMobile() + ", otp=" + getOtp() + ", cookie=" + getCookie() + ", deviceId=" + getDeviceId() + ", sblUserId=" + getSblUserId() + ")";
    }

    @Generated
    public VerifyOtpDTO(final String email, final String password, final String mobile, final String otp, final String cookie, final String deviceId) {
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.otp = otp;
        this.cookie = cookie;
        this.deviceId = deviceId;
    }

    @Generated
    public VerifyOtpDTO(final String email, final String password, final String mobile, final String otp, final String cookie, final String deviceId, final String sblUserId) {
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.otp = otp;
        this.cookie = cookie;
        this.deviceId = deviceId;
        this.sblUserId = sblUserId;
    }

    @Generated
    public String getEmail() {
        return this.email;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }

    @Generated
    public String getMobile() {
        return this.mobile;
    }

    @Generated
    public String getOtp() {
        return this.otp;
    }

    @Generated
    public String getCookie() {
        return this.cookie;
    }

    @Generated
    public String getDeviceId() {
        return this.deviceId;
    }

    @Generated
    public String getSblUserId() {
        return this.sblUserId;
    }
}

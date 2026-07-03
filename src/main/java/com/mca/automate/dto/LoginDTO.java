package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: LoginDTO.class */
public class LoginDTO {
    private String userName;
    private String password;
    private String deviceId;
    private String otp;

    @Generated
    public LoginDTO() {
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
    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    @Generated
    public void setOtp(final String otp) {
        this.otp = otp;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LoginDTO)) {
            return false;
        }
        LoginDTO other = (LoginDTO) o;
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
        if (this$password == null) {
            if (other$password != null) {
                return false;
            }
        } else if (!this$password.equals(other$password)) {
            return false;
        }
        Object this$deviceId = getDeviceId();
        Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null) {
            if (other$deviceId != null) {
                return false;
            }
        } else if (!this$deviceId.equals(other$deviceId)) {
            return false;
        }
        Object this$otp = getOtp();
        Object other$otp = other.getOtp();
        return this$otp == null ? other$otp == null : this$otp.equals(other$otp);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof LoginDTO;
    }

    @Generated
    public int hashCode() {
        Object $userName = getUserName();
        int result = (1 * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result2 = (result * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        int result3 = (result2 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
        Object $otp = getOtp();
        return (result3 * 59) + ($otp == null ? 43 : $otp.hashCode());
    }

    @Generated
    public String toString() {
        return "LoginDTO(userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ", otp=" + getOtp() + ")";
    }

    @Generated
    public String getUserName() {
        return this.userName;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }

    @Generated
    public String getDeviceId() {
        return this.deviceId;
    }

    @Generated
    public String getOtp() {
        return this.otp;
    }
}

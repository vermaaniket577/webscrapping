package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: DirectorDataRequest.class */
public class DirectorDataRequest {
    private String din;
    private String userName;
    private String password;
    private String deviceId;

    @Generated
    public DirectorDataRequest() {
    }

    @Generated
    public void setDin(final String din) {
        this.din = din;
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
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DirectorDataRequest)) {
            return false;
        }
        DirectorDataRequest other = (DirectorDataRequest) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$din = getDin();
        Object other$din = other.getDin();
        if (this$din == null) {
            if (other$din != null) {
                return false;
            }
        } else if (!this$din.equals(other$din)) {
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
        return this$deviceId == null ? other$deviceId == null : this$deviceId.equals(other$deviceId);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof DirectorDataRequest;
    }

    @Generated
    public int hashCode() {
        Object $din = getDin();
        int result = (1 * 59) + ($din == null ? 43 : $din.hashCode());
        Object $userName = getUserName();
        int result2 = (result * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result3 = (result2 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        return (result3 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "DirectorDataRequest(din=" + getDin() + ", userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ")";
    }

    @Generated
    public String getDin() {
        return this.din;
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
}

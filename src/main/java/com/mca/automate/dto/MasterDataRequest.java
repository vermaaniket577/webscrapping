package com.mca.automate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;

/* JADX INFO: loaded from: MasterDataRequest.class */
public class MasterDataRequest {
    @JsonProperty("CompanyNameOrCIN")
    @JsonAlias({"companyNameOrCIN", "companyNameOrCin", "cin", "CIN"})
    private String CompanyNameOrCIN;
    private String userName;
    private String password;
    private String deviceId;

    @Generated
    public MasterDataRequest() {
    }

    @Generated
    public void setCompanyNameOrCIN(final String CompanyNameOrCIN) {
        this.CompanyNameOrCIN = CompanyNameOrCIN;
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
        if (!(o instanceof MasterDataRequest)) {
            return false;
        }
        MasterDataRequest other = (MasterDataRequest) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$CompanyNameOrCIN = getCompanyNameOrCIN();
        Object other$CompanyNameOrCIN = other.getCompanyNameOrCIN();
        if (this$CompanyNameOrCIN == null) {
            if (other$CompanyNameOrCIN != null) {
                return false;
            }
        } else if (!this$CompanyNameOrCIN.equals(other$CompanyNameOrCIN)) {
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
        return other instanceof MasterDataRequest;
    }

    @Generated
    public int hashCode() {
        Object $CompanyNameOrCIN = getCompanyNameOrCIN();
        int result = (1 * 59) + ($CompanyNameOrCIN == null ? 43 : $CompanyNameOrCIN.hashCode());
        Object $userName = getUserName();
        int result2 = (result * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result3 = (result2 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        return (result3 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "MasterDataRequest(CompanyNameOrCIN=" + getCompanyNameOrCIN() + ", userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ")";
    }

    @Generated
    public String getCompanyNameOrCIN() {
        return this.CompanyNameOrCIN;
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

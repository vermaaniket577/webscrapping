package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: NewUserDTO.class */
public class NewUserDTO {
    private String emailId;
    private String mobileNo;
    private String deviceId;
    private Long updatedAt;

    @Generated
    public void setEmailId(final String emailId) {
        this.emailId = emailId;
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
    public void setUpdatedAt(final Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NewUserDTO)) {
            return false;
        }
        NewUserDTO other = (NewUserDTO) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$updatedAt = getUpdatedAt();
        Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null) {
            if (other$updatedAt != null) {
                return false;
            }
        } else if (!this$updatedAt.equals(other$updatedAt)) {
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
        return other instanceof NewUserDTO;
    }

    @Generated
    public int hashCode() {
        Object $updatedAt = getUpdatedAt();
        int result = (1 * 59) + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        Object $emailId = getEmailId();
        int result2 = (result * 59) + ($emailId == null ? 43 : $emailId.hashCode());
        Object $mobileNo = getMobileNo();
        int result3 = (result2 * 59) + ($mobileNo == null ? 43 : $mobileNo.hashCode());
        Object $deviceId = getDeviceId();
        return (result3 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "NewUserDTO(emailId=" + getEmailId() + ", mobileNo=" + getMobileNo() + ", deviceId=" + getDeviceId() + ", updatedAt=" + getUpdatedAt() + ")";
    }

    @Generated
    public NewUserDTO(final String emailId, final String mobileNo, final String deviceId, final Long updatedAt) {
        this.emailId = emailId;
        this.mobileNo = mobileNo;
        this.deviceId = deviceId;
        this.updatedAt = updatedAt;
    }

    @Generated
    public NewUserDTO() {
    }

    @Generated
    public String getEmailId() {
        return this.emailId;
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
    public Long getUpdatedAt() {
        return this.updatedAt;
    }
}

package com.mca.automate.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Generated;

/* JADX INFO: loaded from: NewUserEntity.class */
@Entity
public class NewUserEntity {

    @Id
    private String emailId;
    private String mobileNo;

    @Column(updatable = false)
    private String deviceId;
    private long updatedAt;

    @Generated
    public NewUserEntity(final String emailId, final String mobileNo, final String deviceId, final long updatedAt) {
        this.emailId = emailId;
        this.mobileNo = mobileNo;
        this.deviceId = deviceId;
        this.updatedAt = updatedAt;
    }

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
    public void setUpdatedAt(final long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof NewUserEntity)) {
            return false;
        }
        NewUserEntity other = (NewUserEntity) o;
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
        return other instanceof NewUserEntity;
    }

    @Generated
    public int hashCode() {
        long $updatedAt = getUpdatedAt();
        int result = (1 * 59) + ((int) (($updatedAt >>> 32) ^ $updatedAt));
        Object $emailId = getEmailId();
        int result2 = (result * 59) + ($emailId == null ? 43 : $emailId.hashCode());
        Object $mobileNo = getMobileNo();
        int result3 = (result2 * 59) + ($mobileNo == null ? 43 : $mobileNo.hashCode());
        Object $deviceId = getDeviceId();
        return (result3 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "NewUserEntity(emailId=" + getEmailId() + ", mobileNo=" + getMobileNo() + ", deviceId=" + getDeviceId() + ", updatedAt=" + getUpdatedAt() + ")";
    }

    @Generated
    public NewUserEntity() {
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
    public long getUpdatedAt() {
        return this.updatedAt;
    }
}

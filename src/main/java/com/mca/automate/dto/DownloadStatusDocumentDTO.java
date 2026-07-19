package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: DownloadStatusDocumentDTO.class */
public class DownloadStatusDocumentDTO {
    private String requestType;
    private String referenceNumber;
    private String version;
    private String srn;
    private String dmsId;
    private String userName;
    private String password;
    private String deviceId;

    @Generated
    public void setRequestType(final String requestType) {
        this.requestType = requestType;
    }

    @Generated
    public void setReferenceNumber(final String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    @Generated
    public void setVersion(final String version) {
        this.version = version;
    }

    @Generated
    public void setSrn(final String srn) {
        this.srn = srn;
    }

    @Generated
    public void setDmsId(final String dmsId) {
        this.dmsId = dmsId;
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
        if (!(o instanceof DownloadStatusDocumentDTO)) {
            return false;
        }
        DownloadStatusDocumentDTO other = (DownloadStatusDocumentDTO) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$requestType = getRequestType();
        Object other$requestType = other.getRequestType();
        if (this$requestType == null) {
            if (other$requestType != null) {
                return false;
            }
        } else if (!this$requestType.equals(other$requestType)) {
            return false;
        }
        Object this$referenceNumber = getReferenceNumber();
        Object other$referenceNumber = other.getReferenceNumber();
        if (this$referenceNumber == null) {
            if (other$referenceNumber != null) {
                return false;
            }
        } else if (!this$referenceNumber.equals(other$referenceNumber)) {
            return false;
        }
        Object this$version = getVersion();
        Object other$version = other.getVersion();
        if (this$version == null) {
            if (other$version != null) {
                return false;
            }
        } else if (!this$version.equals(other$version)) {
            return false;
        }
        Object this$srn = getSrn();
        Object other$srn = other.getSrn();
        if (this$srn == null) {
            if (other$srn != null) {
                return false;
            }
        } else if (!this$srn.equals(other$srn)) {
            return false;
        }
        Object this$dmsId = getDmsId();
        Object other$dmsId = other.getDmsId();
        if (this$dmsId == null) {
            if (other$dmsId != null) {
                return false;
            }
        } else if (!this$dmsId.equals(other$dmsId)) {
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
        return other instanceof DownloadStatusDocumentDTO;
    }

    @Generated
    public int hashCode() {
        Object $requestType = getRequestType();
        int result = (1 * 59) + ($requestType == null ? 43 : $requestType.hashCode());
        Object $referenceNumber = getReferenceNumber();
        int result2 = (result * 59) + ($referenceNumber == null ? 43 : $referenceNumber.hashCode());
        Object $version = getVersion();
        int result3 = (result2 * 59) + ($version == null ? 43 : $version.hashCode());
        Object $srn = getSrn();
        int result4 = (result3 * 59) + ($srn == null ? 43 : $srn.hashCode());
        Object $dmsId = getDmsId();
        int result5 = (result4 * 59) + ($dmsId == null ? 43 : $dmsId.hashCode());
        Object $userName = getUserName();
        int result6 = (result5 * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result7 = (result6 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        return (result7 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "DownloadStatusDocumentDTO(requestType=" + getRequestType() + ", referenceNumber=" + getReferenceNumber() + ", version=" + getVersion() + ", srn=" + getSrn() + ", dmsId=" + getDmsId() + ", userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ")";
    }

    @Generated
    public DownloadStatusDocumentDTO(final String requestType, final String referenceNumber, final String version, final String srn, final String dmsId, final String userName, final String password, final String deviceId) {
        this.requestType = requestType;
        this.referenceNumber = referenceNumber;
        this.version = version;
        this.srn = srn;
        this.dmsId = dmsId;
        this.userName = userName;
        this.password = password;
        this.deviceId = deviceId;
    }

    @Generated
    public String getRequestType() {
        return this.requestType;
    }

    @Generated
    public String getReferenceNumber() {
        return this.referenceNumber;
    }

    @Generated
    public String getVersion() {
        return this.version;
    }

    @Generated
    public String getSrn() {
        return this.srn;
    }

    @Generated
    public String getDmsId() {
        return this.dmsId;
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

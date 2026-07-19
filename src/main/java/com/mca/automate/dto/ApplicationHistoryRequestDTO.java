package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: ApplicationHistoryRequestDTO.class */
public class ApplicationHistoryRequestDTO {
    private String requestType;
    private String tab;
    private String cin;
    private String srnno;
    private String fromDate;
    private String toDate;
    private String profUser;
    private String userName;
    private String password;
    private String deviceId;

    @Generated
    public void setRequestType(final String requestType) {
        this.requestType = requestType;
    }

    @Generated
    public void setTab(final String tab) {
        this.tab = tab;
    }

    @Generated
    public void setCin(final String cin) {
        this.cin = cin;
    }

    @Generated
    public void setSrnno(final String srnno) {
        this.srnno = srnno;
    }

    @Generated
    public void setFromDate(final String fromDate) {
        this.fromDate = fromDate;
    }

    @Generated
    public void setToDate(final String toDate) {
        this.toDate = toDate;
    }

    @Generated
    public void setProfUser(final String profUser) {
        this.profUser = profUser;
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
        if (!(o instanceof ApplicationHistoryRequestDTO)) {
            return false;
        }
        ApplicationHistoryRequestDTO other = (ApplicationHistoryRequestDTO) o;
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
        Object this$tab = getTab();
        Object other$tab = other.getTab();
        if (this$tab == null) {
            if (other$tab != null) {
                return false;
            }
        } else if (!this$tab.equals(other$tab)) {
            return false;
        }
        Object this$cin = getCin();
        Object other$cin = other.getCin();
        if (this$cin == null) {
            if (other$cin != null) {
                return false;
            }
        } else if (!this$cin.equals(other$cin)) {
            return false;
        }
        Object this$srnno = getSrnno();
        Object other$srnno = other.getSrnno();
        if (this$srnno == null) {
            if (other$srnno != null) {
                return false;
            }
        } else if (!this$srnno.equals(other$srnno)) {
            return false;
        }
        Object this$fromDate = getFromDate();
        Object other$fromDate = other.getFromDate();
        if (this$fromDate == null) {
            if (other$fromDate != null) {
                return false;
            }
        } else if (!this$fromDate.equals(other$fromDate)) {
            return false;
        }
        Object this$toDate = getToDate();
        Object other$toDate = other.getToDate();
        if (this$toDate == null) {
            if (other$toDate != null) {
                return false;
            }
        } else if (!this$toDate.equals(other$toDate)) {
            return false;
        }
        Object this$profUser = getProfUser();
        Object other$profUser = other.getProfUser();
        if (this$profUser == null) {
            if (other$profUser != null) {
                return false;
            }
        } else if (!this$profUser.equals(other$profUser)) {
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
        return other instanceof ApplicationHistoryRequestDTO;
    }

    @Generated
    public int hashCode() {
        Object $requestType = getRequestType();
        int result = (1 * 59) + ($requestType == null ? 43 : $requestType.hashCode());
        Object $tab = getTab();
        int result2 = (result * 59) + ($tab == null ? 43 : $tab.hashCode());
        Object $cin = getCin();
        int result3 = (result2 * 59) + ($cin == null ? 43 : $cin.hashCode());
        Object $srnno = getSrnno();
        int result4 = (result3 * 59) + ($srnno == null ? 43 : $srnno.hashCode());
        Object $fromDate = getFromDate();
        int result5 = (result4 * 59) + ($fromDate == null ? 43 : $fromDate.hashCode());
        Object $toDate = getToDate();
        int result6 = (result5 * 59) + ($toDate == null ? 43 : $toDate.hashCode());
        Object $profUser = getProfUser();
        int result7 = (result6 * 59) + ($profUser == null ? 43 : $profUser.hashCode());
        Object $userName = getUserName();
        int result8 = (result7 * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result9 = (result8 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        return (result9 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "ApplicationHistoryRequestDTO(requestType=" + getRequestType() + ", tab=" + getTab() + ", cin=" + getCin() + ", srnno=" + getSrnno() + ", fromDate=" + getFromDate() + ", toDate=" + getToDate() + ", profUser=" + getProfUser() + ", userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ")";
    }

    @Generated
    public ApplicationHistoryRequestDTO(final String requestType, final String tab, final String cin, final String srnno, final String fromDate, final String toDate, final String profUser, final String userName, final String password, final String deviceId) {
        this.requestType = requestType;
        this.tab = tab;
        this.cin = cin;
        this.srnno = srnno;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.profUser = profUser;
        this.userName = userName;
        this.password = password;
        this.deviceId = deviceId;
    }

    @Generated
    public String getRequestType() {
        return this.requestType;
    }

    @Generated
    public String getTab() {
        return this.tab;
    }

    @Generated
    public String getCin() {
        return this.cin;
    }

    @Generated
    public String getSrnno() {
        return this.srnno;
    }

    @Generated
    public String getFromDate() {
        return this.fromDate;
    }

    @Generated
    public String getToDate() {
        return this.toDate;
    }

    @Generated
    public String getProfUser() {
        return this.profUser;
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

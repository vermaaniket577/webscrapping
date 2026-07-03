package com.mca.automate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Generated;

/* JADX INFO: loaded from: EditSRNRequestDto.class */
public class EditSRNRequestDto {

    @NotBlank(message = "SRN Number cannot be blank")
    private String formName;

    @NotBlank(message = "Reference Number cannot be blank")
    private String refNo;

    @NotBlank(message = "User Name cannot be blank")
    private String userName;

    @NotBlank(message = "Password  cannot be blank")
    private String password;

    @NotBlank(message = "Device Id cannot be blank")
    private String deviceId;

    @Generated
    public void setFormName(final String formName) {
        this.formName = formName;
    }

    @Generated
    public void setRefNo(final String refNo) {
        this.refNo = refNo;
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
        if (!(o instanceof EditSRNRequestDto)) {
            return false;
        }
        EditSRNRequestDto other = (EditSRNRequestDto) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$formName = getFormName();
        Object other$formName = other.getFormName();
        if (this$formName == null) {
            if (other$formName != null) {
                return false;
            }
        } else if (!this$formName.equals(other$formName)) {
            return false;
        }
        Object this$refNo = getRefNo();
        Object other$refNo = other.getRefNo();
        if (this$refNo == null) {
            if (other$refNo != null) {
                return false;
            }
        } else if (!this$refNo.equals(other$refNo)) {
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
        return other instanceof EditSRNRequestDto;
    }

    @Generated
    public int hashCode() {
        Object $formName = getFormName();
        int result = (1 * 59) + ($formName == null ? 43 : $formName.hashCode());
        Object $refNo = getRefNo();
        int result2 = (result * 59) + ($refNo == null ? 43 : $refNo.hashCode());
        Object $userName = getUserName();
        int result3 = (result2 * 59) + ($userName == null ? 43 : $userName.hashCode());
        Object $password = getPassword();
        int result4 = (result3 * 59) + ($password == null ? 43 : $password.hashCode());
        Object $deviceId = getDeviceId();
        return (result4 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
    }

    @Generated
    public String toString() {
        return "EditSRNRequestDto(formName=" + getFormName() + ", refNo=" + getRefNo() + ", userName=" + getUserName() + ", password=" + getPassword() + ", deviceId=" + getDeviceId() + ")";
    }

    @Generated
    public EditSRNRequestDto(final String formName, final String refNo, final String userName, final String password, final String deviceId) {
        this.formName = formName;
        this.refNo = refNo;
        this.userName = userName;
        this.password = password;
        this.deviceId = deviceId;
    }

    @Generated
    public String getFormName() {
        return this.formName;
    }

    @Generated
    public String getRefNo() {
        return this.refNo;
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

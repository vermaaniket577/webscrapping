package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: CookieDTO.class */
public class CookieDTO {
    private String csrfToken;
    private String cookiesession1;
    private String uuidHASH;
    private String deviceId;
    private String sessiontokenmd5;
    private String sessionID;

    @Generated
    public void setCsrfToken(final String csrfToken) {
        this.csrfToken = csrfToken;
    }

    @Generated
    public void setCookiesession1(final String cookiesession1) {
        this.cookiesession1 = cookiesession1;
    }

    @Generated
    public void setUuidHASH(final String uuidHASH) {
        this.uuidHASH = uuidHASH;
    }

    @Generated
    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
    }

    @Generated
    public void setSessiontokenmd5(final String sessiontokenmd5) {
        this.sessiontokenmd5 = sessiontokenmd5;
    }

    @Generated
    public void setSessionID(final String sessionID) {
        this.sessionID = sessionID;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CookieDTO)) {
            return false;
        }
        CookieDTO other = (CookieDTO) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Object this$csrfToken = getCsrfToken();
        Object other$csrfToken = other.getCsrfToken();
        if (this$csrfToken == null) {
            if (other$csrfToken != null) {
                return false;
            }
        } else if (!this$csrfToken.equals(other$csrfToken)) {
            return false;
        }
        Object this$cookiesession1 = getCookiesession1();
        Object other$cookiesession1 = other.getCookiesession1();
        if (this$cookiesession1 == null) {
            if (other$cookiesession1 != null) {
                return false;
            }
        } else if (!this$cookiesession1.equals(other$cookiesession1)) {
            return false;
        }
        Object this$uuidHASH = getUuidHASH();
        Object other$uuidHASH = other.getUuidHASH();
        if (this$uuidHASH == null) {
            if (other$uuidHASH != null) {
                return false;
            }
        } else if (!this$uuidHASH.equals(other$uuidHASH)) {
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
        Object this$sessiontokenmd5 = getSessiontokenmd5();
        Object other$sessiontokenmd5 = other.getSessiontokenmd5();
        if (this$sessiontokenmd5 == null) {
            if (other$sessiontokenmd5 != null) {
                return false;
            }
        } else if (!this$sessiontokenmd5.equals(other$sessiontokenmd5)) {
            return false;
        }
        Object this$sessionID = getSessionID();
        Object other$sessionID = other.getSessionID();
        return this$sessionID == null ? other$sessionID == null : this$sessionID.equals(other$sessionID);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof CookieDTO;
    }

    @Generated
    public int hashCode() {
        Object $csrfToken = getCsrfToken();
        int result = (1 * 59) + ($csrfToken == null ? 43 : $csrfToken.hashCode());
        Object $cookiesession1 = getCookiesession1();
        int result2 = (result * 59) + ($cookiesession1 == null ? 43 : $cookiesession1.hashCode());
        Object $uuidHASH = getUuidHASH();
        int result3 = (result2 * 59) + ($uuidHASH == null ? 43 : $uuidHASH.hashCode());
        Object $deviceId = getDeviceId();
        int result4 = (result3 * 59) + ($deviceId == null ? 43 : $deviceId.hashCode());
        Object $sessiontokenmd5 = getSessiontokenmd5();
        int result5 = (result4 * 59) + ($sessiontokenmd5 == null ? 43 : $sessiontokenmd5.hashCode());
        Object $sessionID = getSessionID();
        return (result5 * 59) + ($sessionID == null ? 43 : $sessionID.hashCode());
    }

    @Generated
    public String toString() {
        return "CookieDTO(csrfToken=" + getCsrfToken() + ", cookiesession1=" + getCookiesession1() + ", uuidHASH=" + getUuidHASH() + ", deviceId=" + getDeviceId() + ", sessiontokenmd5=" + getSessiontokenmd5() + ", sessionID=" + getSessionID() + ")";
    }

    @Generated
    public CookieDTO(final String csrfToken, final String cookiesession1, final String uuidHASH, final String deviceId, final String sessiontokenmd5, final String sessionID) {
        this.csrfToken = csrfToken;
        this.cookiesession1 = cookiesession1;
        this.uuidHASH = uuidHASH;
        this.deviceId = deviceId;
        this.sessiontokenmd5 = sessiontokenmd5;
        this.sessionID = sessionID;
    }

    @Generated
    public String getCsrfToken() {
        return this.csrfToken;
    }

    @Generated
    public String getCookiesession1() {
        return this.cookiesession1;
    }

    @Generated
    public String getUuidHASH() {
        return this.uuidHASH;
    }

    @Generated
    public String getDeviceId() {
        return this.deviceId;
    }

    @Generated
    public String getSessiontokenmd5() {
        return this.sessiontokenmd5;
    }

    @Generated
    public String getSessionID() {
        return this.sessionID;
    }
}

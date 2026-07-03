package com.mca.automate.dto;

import lombok.Generated;

/* JADX INFO: loaded from: ApiResponse.class */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    @Generated
    public void setCode(final int code) {
        this.code = code;
    }

    @Generated
    public void setMessage(final String message) {
        this.message = message;
    }

    @Generated
    public void setData(final T data) {
        this.data = data;
    }

    @Generated
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ApiResponse)) {
            return false;
        }
        ApiResponse<?> other = (ApiResponse) o;
        if (!other.canEqual(this) || getCode() != other.getCode()) {
            return false;
        }
        Object this$message = getMessage();
        Object other$message = other.getMessage();
        if (this$message == null) {
            if (other$message != null) {
                return false;
            }
        } else if (!this$message.equals(other$message)) {
            return false;
        }
        Object this$data = getData();
        Object other$data = other.getData();
        return this$data == null ? other$data == null : this$data.equals(other$data);
    }

    @Generated
    protected boolean canEqual(final Object other) {
        return other instanceof ApiResponse;
    }

    @Generated
    public int hashCode() {
        int result = (1 * 59) + getCode();
        Object $message = getMessage();
        int result2 = (result * 59) + ($message == null ? 43 : $message.hashCode());
        Object $data = getData();
        return (result2 * 59) + ($data == null ? 43 : $data.hashCode());
    }

    @Generated
    public String toString() {
        return "ApiResponse(code=" + getCode() + ", message=" + getMessage() + ", data=" + getData() + ")";
    }

    @Generated
    public ApiResponse(final int code, final String message, final T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    @Generated
    public int getCode() {
        return this.code;
    }

    @Generated
    public String getMessage() {
        return this.message;
    }

    @Generated
    public T getData() {
        return this.data;
    }
}

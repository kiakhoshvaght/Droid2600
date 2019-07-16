package ModelClasses;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BarangErrorResponse extends Exception{

    @SerializedName("Code")
    @Expose
    private Integer code;
    @SerializedName("Message")
    @Expose
    private String message;
    @SerializedName("ResponseUtcTime")
    @Expose
    private String responseUtcTime;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResponseUtcTime() {
        return responseUtcTime;
    }

    public void setResponseUtcTime(String responseUtcTime) {
        this.responseUtcTime = responseUtcTime;
    }
}
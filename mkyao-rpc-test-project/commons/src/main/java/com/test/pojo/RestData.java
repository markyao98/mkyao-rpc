package com.test.pojo;



import java.io.Serializable;


public class RestData implements Serializable {
    private Boolean success;
    private Integer code;
    private String msg;
    private Object data;


    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public RestData(Boolean success, Integer code, String msg, Object data) {
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public RestData() {
    }

    public static RestData success(Object data){
        return new RestData(true,200,"成功",data);
    }

    public static RestData fails(String msg){
        return new RestData(false,500,"失败",null);
    }
}

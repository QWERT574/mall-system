package com.example.minimall.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求体（小程序扫码登录场景）。
 *
 * <p>必填：openid；可选：nickname、avatar。
 */
public class LoginRequest {
    @NotBlank(message = "openid不能为空")
    private String openid;
    
    private String nickname;
    
    private String avatar;
    
    public String getOpenid() {
        return openid;
    }
    
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}

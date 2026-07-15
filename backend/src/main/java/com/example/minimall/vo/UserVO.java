package com.example.minimall.vo;

/**
 * 用户信息视图对象：用于对外接口（不含敏感字段），含昵称、头像、联系方式、用户类型、认证状态等。
 */
public class UserVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer userType;
    private String companyName;
    private Integer isVerified;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getUserType() {
        return userType;
    }
    
    public void setUserType(Integer userType) {
        this.userType = userType;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public Integer getIsVerified() {
        return isVerified;
    }
    
    public void setIsVerified(Integer isVerified) {
        this.isVerified = isVerified;
    }
}

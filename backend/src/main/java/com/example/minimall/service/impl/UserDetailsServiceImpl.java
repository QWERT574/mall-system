package com.example.minimall.service.impl;

import com.example.minimall.model.User;
import com.example.minimall.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Spring Security 用户认证信息加载实现 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /** 用户Mapper */
    @Autowired
    private UserMapper userMapper;

    @Override
    /**
     * Spring Security 加载用户认证信息
     * <p>
     * 查找顺序：openid → 手机号 → 用户名。找到后根据 userType 分配角色：
     * <ul>
     *   <li>2 → ROLE_ADMIN + ROLE_USER</li>
     *   <li>1 → ROLE_SELLER + ROLE_USER</li>
     *   <li>其它 → ROLE_BUYER + ROLE_USER</li>
     * </ul>
     * </p>
     *
     * @param username openid / 手机号 / 用户名
     * @return Spring Security UserDetails
     * @throws UsernameNotFoundException 用户不存在
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = null;
        
        // 尝试先按openid查询
        user = userMapper.selectByOpenid(username);
        
        // 如果按openid查询不到，尝试按手机号查询
        if (user == null) {
            user = userMapper.selectByPhone(username);
        }
        
        // 如果按手机号查询不到，尝试按用户名查询
        if (user == null) {
            user = userMapper.selectByUsername(username);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        if (user.getUserType() == 2) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        } else if (user.getUserType() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_BUYER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        // 确定用户名（优先使用openid，否则使用用户名，最后使用手机号）
        String principal = user.getOpenid() != null ? user.getOpenid() : (user.getUsername() != null ? user.getUsername() : user.getPhone());
        
        // 创建并返回UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                principal,
                user.getPassword() != null ? user.getPassword() : "password", // 使用用户实际密码或默认值
                authorities
        );
    }
}
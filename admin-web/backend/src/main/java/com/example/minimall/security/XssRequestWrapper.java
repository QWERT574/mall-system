package com.example.minimall.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 防护请求包装器。
 * <p>
 * 继承自 {@link HttpServletRequestWrapper}，重写参数与请求头读取方法，
 * 在调用方取值时使用 {@link XssUtils#sanitize(String)} 进行清洗，
 * 配合 {@code XssFilter} 实现对 /api/** 的透明 XSS 防护。
 * </p>
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 构造方法，包装原始 HttpServletRequest。
     */
    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 获取指定参数名对应的多值数组，统一对每个值进行 XSS 清洗。
     */
    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = XssUtils.sanitize(values[i]);
        }
        
        return encodedValues;
    }
    
    /**
     * 获取指定参数名对应的单值，并进行 XSS 清洗。
     */
    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        if (value == null) {
            return null;
        }
        return XssUtils.sanitize(value);
    }
    
    /**
     * 获取全部参数 Map，遍历后对每个值进行 XSS 清洗后返回安全副本。
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> paramMap = super.getParameterMap();
        Map<String, String[]> secureMap = new HashMap<>();
        
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null) {
                secureMap.put(entry.getKey(), null);
                continue;
            }
            
            String[] encodedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                encodedValues[i] = XssUtils.sanitize(values[i]);
            }
            secureMap.put(entry.getKey(), encodedValues);
        }
        
        return secureMap;
    }
    
    /**
     * 获取请求头并对值进行 XSS 清洗，避免恶意脚本/事件处理器注入。
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null) {
            return null;
        }
        return XssUtils.sanitize(value);
    }
}

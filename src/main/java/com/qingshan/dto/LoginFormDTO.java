package com.qingshan.dto;

import lombok.Data;

/**
 * 登录表单传输实体
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}

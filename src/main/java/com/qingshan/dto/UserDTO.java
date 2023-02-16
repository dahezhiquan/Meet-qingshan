package com.qingshan.dto;

import lombok.Data;

/**
 * 用户信息返回实体
 */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}

package com.gaokao.system.vo;

import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
public class LoginVO {

    /**
     * Token
     */
    private String token;

    /**
     * Token 类型
     */
    private String tokenType;

    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfoVO userInfo;

    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String phone;
        private String province;
        private String grade;
        private String subjects;
        private Integer targetScore;
    }
}
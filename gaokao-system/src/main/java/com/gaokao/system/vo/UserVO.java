package com.gaokao.system.vo;

import lombok.Data;

/**
 * 用户信息 VO
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private Integer gender;

    private String province;

    private String grade;

    private String subjects;

    private Integer targetScore;

    private Integer status;
}
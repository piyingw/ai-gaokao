package com.gaokao.system.service;

import com.gaokao.system.dto.LoginDTO;
import com.gaokao.system.dto.RegisterDTO;
import com.gaokao.system.vo.LoginVO;
import com.gaokao.system.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     * @return 用户 ID
     */
    Long register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 登录信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 退出登录
     *
     * @param token Token
     */
    void logout(String token);

    /**
     * 获取用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId 用户 ID
     * @param vo     用户信息
     */
    void updateUserInfo(Long userId, UserVO vo);

    /**
     * 修改密码
     *
     * @param userId    用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
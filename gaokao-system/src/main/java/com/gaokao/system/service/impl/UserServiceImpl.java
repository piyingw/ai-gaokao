package com.gaokao.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gaokao.common.constant.SystemConstants;
import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.ResultCode;
import com.gaokao.common.service.VerifyCodeService;
import com.gaokao.common.utils.JwtUtils;
import com.gaokao.system.dto.LoginDTO;
import com.gaokao.system.dto.RegisterDTO;
import com.gaokao.system.entity.User;
import com.gaokao.system.mapper.UserMapper;
import com.gaokao.system.service.UserService;
import com.gaokao.system.vo.LoginVO;
import com.gaokao.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final VerifyCodeService verifyCodeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // 1. 验证邮箱验证码
        if (!verifyCodeService.verifyCode(dto.getEmail(), dto.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }

        // 2. 检查用户名是否已存在
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "用户名已存在");
        }

        // 3. 检查邮箱是否已存在
        count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, dto.getEmail()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "邮箱已注册");
        }

        // 4. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encodePassword(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        log.info("用户注册成功：{}", user.getId());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户 - 支持用户名或邮箱登录
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper.eq(User::getUsername, dto.getUsername())
                        .or()
                        .eq(User::getEmail, dto.getUsername())));

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 验证密码
        if (!checkPassword(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 4. 生成 Token
        String token = JwtUtils.generateToken(user.getId());

        // 5. 保存 Token 到 Redis
        String redisKey = SystemConstants.RedisKey.USER_TOKEN + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, SystemConstants.TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);

        // 6. 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType("Bearer");
        loginVO.setExpiresIn(SystemConstants.TOKEN_EXPIRE_HOURS * 3600);
        loginVO.setUserInfo(convertToUserInfoVO(user));

        log.info("用户登录成功：{}", user.getId());
        return loginVO;
    }

    @Override
    public void logout(String token) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token);
            String redisKey = SystemConstants.RedisKey.USER_TOKEN + userId;
            redisTemplate.delete(redisKey);
            log.info("用户退出登录：{}", userId);
        } catch (Exception e) {
            log.warn("退出登录失败：{}", e.getMessage());
        }
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setNickname(vo.getNickname());
        user.setAvatar(vo.getAvatar());
        user.setEmail(vo.getEmail());
        user.setGender(vo.getGender());
        user.setProvince(vo.getProvince());
        user.setGrade(vo.getGrade());
        user.setSubjects(vo.getSubjects());
        user.setTargetScore(vo.getTargetScore());

        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!checkPassword(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "原密码错误");
        }

        user.setPassword(encodePassword(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 密码加密（简单实现，生产环境建议使用 BCrypt）
     */
    private String encodePassword(String password) {
        // TODO: 生产环境使用 BCrypt
        return com.gaokao.common.utils.DigestUtils.md5Hex(password);
    }

    /**
     * 密码校验
     */
    private boolean checkPassword(String rawPassword, String encodedPassword) {
        return encodePassword(rawPassword).equals(encodedPassword);
    }

    private LoginVO.UserInfoVO convertToUserInfoVO(User user) {
        LoginVO.UserInfoVO vo = new LoginVO.UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setProvince(user.getProvince());
        vo.setGrade(user.getGrade());
        vo.setSubjects(user.getSubjects());
        vo.setTargetScore(user.getTargetScore());
        return vo;
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setProvince(user.getProvince());
        vo.setGrade(user.getGrade());
        vo.setSubjects(user.getSubjects());
        vo.setTargetScore(user.getTargetScore());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
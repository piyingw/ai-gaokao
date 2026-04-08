package com.gaokao.web.controller;

import com.gaokao.common.exception.BusinessException;
import com.gaokao.common.result.Result;
import com.gaokao.common.result.ResultCode;
import com.gaokao.common.service.VerifyCodeService;
import com.gaokao.system.dto.LoginDTO;
import com.gaokao.system.dto.RegisterDTO;
import com.gaokao.system.service.UserService;
import com.gaokao.system.vo.LoginVO;
import com.gaokao.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = userService.register(dto);
        return Result.success(userId);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success(loginVO);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        String realToken = token.replace("Bearer ", "");
        userService.logout(realToken);
        return Result.success();
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(
            @RequestAttribute("userId") Long userId,
            @RequestBody UserVO vo) {
        userService.updateUserInfo(userId, vo);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestAttribute("userId") Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    @Operation(summary = "发送邮箱验证码", description = "发送6位数字验证码到指定邮箱，有效期3分钟")
    @PostMapping("/code/email")
    public Result<Void> sendEmailCode(
            @Parameter(description = "邮箱地址") @RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        boolean success = verifyCodeService.sendCodeToEmail(email);
        if (!success) {
            throw new BusinessException(ResultCode.ERROR, "验证码发送失败，请稍后重试");
        }
        return Result.success();
    }
}
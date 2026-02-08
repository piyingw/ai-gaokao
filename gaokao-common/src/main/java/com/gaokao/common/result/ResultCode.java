package com.gaokao.common.result;

import lombok.Getter;

/**
 * 统一响应状态码
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),

    // 业务错误 5xx
    ERROR(500, "系统内部错误"),
    
    // 通用业务错误 1xxx
    PARAM_ERROR(1001, "参数校验失败"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_DUPLICATE(1003, "数据已存在"),
    DATA_ERROR(1004, "数据异常"),
    OPERATION_FAILED(1005, "操作失败"),
    
    // 用户相关 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "用户名或密码错误"),
    USER_DISABLED(2003, "用户已被禁用"),
    TOKEN_EXPIRED(2004, "Token 已过期"),
    TOKEN_INVALID(2005, "Token 无效"),
    
    // AI 相关 3xxx
    AI_SERVICE_ERROR(3001, "AI 服务异常"),
    AI_RATE_LIMIT(3002, "AI 请求频率超限"),
    AI_TIMEOUT(3003, "AI 响应超时"),
    
    // 数据相关 4xxx
    DATA_IMPORT_ERROR(4001, "数据导入失败"),
    DATA_EXPORT_ERROR(4002, "数据导出失败"),
    DATA_SYNC_ERROR(4003, "数据同步失败"),
    SYSTEM_ERROR(4004, "系统错误"),

    // 订单相关 5xxx
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_STATUS_ERROR(5002, "订单状态异常"),
    ORDER_TIMEOUT(5003, "订单已超时"),
    ORDER_ALREADY_PAID(5004, "订单已支付"),
    ORDER_PAYMENT_FAILED(5005, "支付失败"),
    ORDER_REFUND_FAILED(5006, "退款失败"),

    // 会员相关 6xxx
    MEMBER_NOT_FOUND(6001, "会员信息不存在"),
    MEMBER_EXPIRED(6002, "会员已过期"),
    MEMBER_LEVEL_NOT_ENOUGH(6003, "会员等级不足"),
    PRIVILEGE_LIMIT_EXCEEDED(6004, "权益使用次数已达上限");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
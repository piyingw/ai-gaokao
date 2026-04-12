package com.gaokao.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加密工具类
 */
public class DigestUtils {

    private DigestUtils() {
    }

    /**
     * MD5 加密
     *
     * @param data 原始数据
     * @return 加密后的十六进制字符串
     */
    public static String md5Hex(String data) {
        return digest("MD5", data);
    }

    /**
     * SHA-256 加密
     *
     * @param data 原始数据
     * @return 加密后的十六进制字符串
     */
    public static String sha256Hex(String data) {
        return digest("SHA-256", data);
    }

    /**
     * 摘要算法
     *
     * @param algorithm 算法名称
     * @param data      原始数据
     * @return 加密后的十六进制字符串
     */
    private static String digest(String algorithm, String data) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的加密算法：" + algorithm, e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
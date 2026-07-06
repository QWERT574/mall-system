package com.example.minimall.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 密码复杂度校验器
 *
 * 规则:
 *   1. 长度 8-32 位
 *   2. 必须同时包含字母和数字
 *   3. 不在常见弱密码字典中
 *   4. 不能与用户名/手机号相同
 */
public class PasswordValidator {

    private static final List<String> WEAK_PASSWORDS = Arrays.asList(
        "12345678", "123456789", "1234567890", "00000000", "11111111",
        "22222222", "33333333", "44444444", "55555555", "66666666",
        "77777777", "88888888", "99999999", "01234567", "98765432",
        "qwertyui", "asdfghjk", "zxcvbnm1", "qazwsxedc",
        "password", "password1", "password123", "p@ssw0rd", "passw0rd",
        "iloveyou", "welcome", "welcome1", "admin123", "abc12345",
        "abcd1234", "qwerty123", "asdf1234", "zxcv1234", "1q2w3e4r",
        "qweasdzxc", "qwertyuiop", "asdfghjkl", "12341234", "11223344",
        "abcdefgh", "abcdefg1", "abcd1234", "aa123456", "a1234567",
        "letmein1", "trustno1", "dragon12", "monkey12", "master12",
        "shadow12", "michael1", "jennifer", "jordan23", "superman1",
        "batman12", "11111111", "14725836", "147258369", "159753123"
    );

    public enum Rule {
        LENGTH("长度 8-32 位"),
        MIX_LETTER_NUMBER("同时包含字母和数字"),
        NOT_WEAK("不能是常见弱密码"),
        NOT_SAME_AS_ACCOUNT("不能与用户名/手机号相同");

        public final String label;
        Rule(String label) { this.label = label; }
    }

    /**
     * 单条规则校验结果。
     */
    public static class CheckResult {
        public final Rule rule;
        public final boolean pass;
        public final String message;

        public CheckResult(Rule rule, boolean pass, String message) {
            this.rule = rule;
            this.pass = pass;
            this.message = message;
        }
    }

    /**
     * 执行全部规则校验并返回逐条结果。
     *
     * @param password 待校验密码
     * @param username 关联用户名（用于排除与账号相同）
     * @param phone    关联手机号（用于排除与账号相同）
     * @return 每条规则的校验结果列表
     */
    public static List<CheckResult> check(String password, String username, String phone) {
        List<CheckResult> results = new ArrayList<>();

        boolean lengthOk = password != null
            && password.length() >= 8
            && password.length() <= 32;
        results.add(new CheckResult(Rule.LENGTH, lengthOk,
            lengthOk ? "长度合适" : "长度需 8-32 位"));

        boolean mixOk = password != null
            && password.matches(".*[A-Za-z].*")
            && password.matches(".*[0-9].*");
        results.add(new CheckResult(Rule.MIX_LETTER_NUMBER, mixOk,
            mixOk ? "已包含字母和数字" : "需同时包含字母和数字"));

        boolean weakOk = password != null
            && !WEAK_PASSWORDS.contains(password.toLowerCase());
        results.add(new CheckResult(Rule.NOT_WEAK, weakOk,
            weakOk ? "不在常见弱密码列表" : "太常见,易被破解"));

        boolean sameAsAccount = password != null && (
            (username != null && !username.isEmpty() && password.equalsIgnoreCase(username))
            || (phone != null && !phone.isEmpty() && password.equals(phone))
            || (phone != null && !phone.isEmpty() && phone.length() >= 6
                && password.equals(phone.substring(phone.length() - 6)))
        );
        results.add(new CheckResult(Rule.NOT_SAME_AS_ACCOUNT, !sameAsAccount,
            sameAsAccount ? "不能与用户名/手机号相同" : "与账号信息不同"));

        return results;
    }

    /**
     * 校验密码，校验通过返回 null；不通过返回第一条失败规则的中文提示。
     *
     * @param password 待校验密码
     * @param username 关联用户名
     * @param phone    关联手机号
     * @return 失败信息；通过则返回 null
     */
    public static String validate(String password, String username, String phone) {
        if (password == null || password.isEmpty()) {
            return "请输入密码";
        }
        List<CheckResult> results = check(password, username, phone);
        for (CheckResult r : results) {
            if (!r.pass) {
                return r.message;
            }
        }
        return null;
    }
}

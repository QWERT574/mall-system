package com.example.minimall.controller;

import com.example.minimall.annotation.RateLimit;
import com.example.minimall.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码
 * - GET /api/captcha/image?key=xxx  返回 base64 PNG
 * - 前端把 key + 用户输入的 code 一并提交到登录接口
 * - 这里使用内存 ConcurrentHashMap 存储（5 分钟过期），无外部依赖
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController extends BaseController {

    /** key -> (答案, 过期时间戳) */
    private static final Map<String, CodeEntry> CODE_STORE = new ConcurrentHashMap<>();
    /** 验证码有效期：5 分钟 */
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    /** 字符集（去掉容易混淆的 0/O/1/l/I） */
    private static final char[] CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LEN = 4;
    private static final int IMG_WIDTH = 130;
    private static final int IMG_HEIGHT = 48;

    /** 生成图形验证码并返回 base64 PNG 与 key */
    @RateLimit(limit = 30, timeout = 60)
    @GetMapping("/image")
    public Result<Map<String, Object>> image(@RequestParam(required = false) String key) {
        // 1. 校验/生成 key
        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString().replace("-", "");
        }

        // 2. 生成随机码
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) {
            code.append(CHARS[random.nextInt(CHARS.length)]);
        }
        String codeStr = code.toString();

        // 3. 存进内存（覆盖旧值）
        CODE_STORE.put(key, new CodeEntry(codeStr, System.currentTimeMillis() + TTL_MILLIS));

        // 4. 清理过期
        cleanupExpired();

        // 5. 绘制图片并转 base64
        try {
            String base64 = drawCaptchaImage(codeStr);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("key", key);
            data.put("image", "data:image/png;base64," + base64);
            return success(data);
        } catch (Exception e) {
            return error("生成验证码失败：" + e.getMessage());
        }
    }

    /**
     * 验证图形验证码，供其他 controller 内部调用
     * @return 0=通过, 1=不存在/已过期, 2=错误
     */
    public static int verify(String key, String userInput) {
        if (key == null || userInput == null) return 1;
        CodeEntry entry = CODE_STORE.get(key);
        if (entry == null) return 1;
        if (System.currentTimeMillis() > entry.expireAt) {
            CODE_STORE.remove(key);
            return 1;
        }
        // 验证码不区分大小写
        if (!entry.code.equalsIgnoreCase(userInput.trim())) {
            return 2;
        }
        // 一次性使用：验证通过后立即删除
        CODE_STORE.remove(key);
        return 0;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        CODE_STORE.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

    /**
     * 绘制带干扰线和噪点的验证码图片
     */
    private String drawCaptchaImage(String code) throws Exception {
        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 背景渐变
        GradientPaint bg = new GradientPaint(0, 0, new Color(245, 240, 232), IMG_WIDTH, IMG_HEIGHT, new Color(230, 220, 200));
        g.setPaint(bg);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        Random random = new Random();

        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(120 + random.nextInt(80), 100 + random.nextInt(80), 80 + random.nextInt(60), 120));
            g.setStroke(new BasicStroke(1.2f));
            int x1 = random.nextInt(IMG_WIDTH);
            int y1 = random.nextInt(IMG_HEIGHT);
            int x2 = random.nextInt(IMG_WIDTH);
            int y2 = random.nextInt(IMG_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 噪点
        for (int i = 0; i < 60; i++) {
            int x = random.nextInt(IMG_WIDTH);
            int y = random.nextInt(IMG_HEIGHT);
            g.setColor(new Color(80 + random.nextInt(100), 60 + random.nextInt(100), 40 + random.nextInt(100)));
            g.fillRect(x, y, 1, 1);
        }

        // 字符：每个字单独旋转 + 随机颜色
        int charWidth = IMG_WIDTH / (CODE_LEN + 1);
        String[] fonts = {"Arial", "Georgia", "Times New Roman", "Verdana", "Courier New"};
        for (int i = 0; i < code.length(); i++) {
            String ch = String.valueOf(code.charAt(i));
            g.setFont(new Font(fonts[random.nextInt(fonts.length)], Font.BOLD, 26 + random.nextInt(8)));
            g.setColor(new Color(40 + random.nextInt(80), 30 + random.nextInt(60), 20 + random.nextInt(60)));

            double angle = (random.nextInt(60) - 30) * Math.PI / 180;
            int x = charWidth * (i + 1) - 12;
            int y = 32 + random.nextInt(6);
            g.rotate(angle, x, y);
            g.drawString(ch, x, y);
            g.rotate(-angle, x, y);
        }

        // 边框
        g.setColor(new Color(180, 165, 140));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, IMG_WIDTH - 1, IMG_HEIGHT - 1);

        g.dispose();

        // 转 base64 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static class CodeEntry {
        final String code;
        final long expireAt;
        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}

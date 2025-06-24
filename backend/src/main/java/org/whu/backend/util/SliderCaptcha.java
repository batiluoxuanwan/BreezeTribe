package org.whu.backend.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;


public class SliderCaptcha {

    public static CaptchaData generateCaptcha() throws IOException {
        BufferedImage original = ImageIO.read(new File("bg.png"));
        int targetX = new Random().nextInt(original.getWidth() - 50);
        int targetY = new Random().nextInt(original.getHeight() - 50);

        // 创建滑块片
        BufferedImage slide = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = slide.createGraphics();
        g.drawImage(original,
                0, 0, 50, 50,
                targetX, targetY, targetX + 50, targetY + 50,
                null);
        g.dispose();

        // 在背景绘制缺口区域
        Graphics2D g2 = original.createGraphics();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g2.setColor(Color.BLACK);
        g2.fillRect(targetX, targetY, 50, 50);
        g2.dispose();

        // 生成唯一ID
        String captchaId = UUID.randomUUID().toString();

        return new CaptchaData(
                captchaId,
                imageToBase64(original),
                imageToBase64(slide),
                targetX
        );
    }

    private static String imageToBase64(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}

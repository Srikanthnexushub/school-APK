package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.CaptchaChallengeResponse;
import com.edutech.auth.domain.port.out.CaptchaChallengeStore;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
public class LocalCaptchaService {

    // Unambiguous chars — no I/1/O/0 confusion
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaChallengeStore captchaChallengeStore;

    public LocalCaptchaService(CaptchaChallengeStore captchaChallengeStore) {
        this.captchaChallengeStore = captchaChallengeStore;
    }

    public CaptchaChallengeResponse generate() {
        String answer = randomText();
        String id = UUID.randomUUID().toString();
        captchaChallengeStore.save(id, answer);
        String imageDataUri = "data:image/png;base64," + buildImageBase64(answer);
        return new CaptchaChallengeResponse(id, imageDataUri);
    }

    private String randomText() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String buildImageBase64(String text) {
        int w = 220, h = 80;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark background
        g.setColor(new Color(22, 22, 35));
        g.fillRect(0, 0, w, h);

        // Noise dots
        for (int i = 0; i < 400; i++) {
            int alpha = 60 + RANDOM.nextInt(80);
            g.setColor(new Color(100 + RANDOM.nextInt(100), 100 + RANDOM.nextInt(100), 130 + RANDOM.nextInt(100), alpha));
            int dotSize = RANDOM.nextInt(2) + 1;
            g.fillOval(RANDOM.nextInt(w), RANDOM.nextInt(h), dotSize, dotSize);
        }

        // Wavy distortion lines across the image
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(70 + RANDOM.nextInt(80), 90 + RANDOM.nextInt(100), 150 + RANDOM.nextInt(80), 160));
            g.setStroke(new BasicStroke(1.5f));
            int y1 = 10 + RANDOM.nextInt(h - 20);
            int y2 = 10 + RANDOM.nextInt(h - 20);
            int y3 = 10 + RANDOM.nextInt(h - 20);
            int y4 = 10 + RANDOM.nextInt(h - 20);
            g.drawLine(0, y1, w / 3, y2);
            g.drawLine(w / 3, y2, 2 * w / 3, y3);
            g.drawLine(2 * w / 3, y3, w, y4);
        }

        // Draw each character with random rotation, position jitter, and color
        int charSlotW = w / (LENGTH + 1);
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            // Vary font size slightly per char
            int fontSize = 30 + RANDOM.nextInt(10);
            Font font = new Font("SansSerif", Font.BOLD, fontSize);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();

            int x = charSlotW / 2 + charSlotW * i + RANDOM.nextInt(6) - 3;
            int baseY = (h + fm.getAscent()) / 2 - fm.getDescent() / 2;
            int yOffset = RANDOM.nextInt(14) - 7;
            double angle = Math.toRadians(RANDOM.nextInt(40) - 20);

            AffineTransform orig = g.getTransform();
            g.rotate(angle, x, baseY + yOffset);

            // Drop shadow
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(ch, x + 2, baseY + yOffset + 2);

            // Character with bright color
            g.setColor(new Color(
                170 + RANDOM.nextInt(85),
                170 + RANDOM.nextInt(85),
                170 + RANDOM.nextInt(85)
            ));
            g.drawString(ch, x, baseY + yOffset);
            g.setTransform(orig);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CAPTCHA image", e);
        }
    }
}

package ai.mindvex.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@mindvex.ai}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Send a welcome email to new users
     */
    @Async
    public void sendWelcomeEmail(String to, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to MindVex!");
            helper.setText(buildWelcomeEmailBody(fullName), true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    /**
     * Build HTML email body for welcome email
     */
    private String buildWelcomeEmailBody(String fullName) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #1a1a2e;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                        <div style="background: linear-gradient(135deg, #16213e 0%%, #1a1a2e 100%%); border-radius: 16px; padding: 40px; border: 1px solid #ff6b35;">

                            <!-- Logo/Header -->
                            <div style="text-align: center; margin-bottom: 30px;">
                                <h1 style="color: #ff6b35; font-size: 32px; margin: 0; font-weight: 700;">MindVex</h1>
                                <p style="color: #8892b0; font-size: 14px; margin-top: 8px;">Your AI Development Platform</p>
                            </div>

                            <!-- Main Content -->
                            <div style="text-align: center;">
                                <h2 style="color: #ffffff; font-size: 24px; margin-bottom: 16px;">Welcome, %s!</h2>
                                <p style="color: #8892b0; font-size: 16px; line-height: 1.6; margin-bottom: 30px;">
                                    Your account has been created successfully. Start building amazing things with MindVex!
                                </p>

                                <!-- CTA Button -->
                                <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #ff6b35 0%%, #ff8c61 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 15px rgba(255, 107, 53, 0.3);">
                                    Get Started
                                </a>
                            </div>

                            <!-- Footer -->
                            <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #2d3a4f;">
                                <p style="color: #5a6a8a; font-size: 12px; margin-top: 8px;">
                                    &copy; 2026 MindVex. All rights reserved.
                                </p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(fullName, frontendUrl);
    }
}

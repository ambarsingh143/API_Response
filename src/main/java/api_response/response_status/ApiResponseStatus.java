package api_response.response_status;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.mail.*;
import javax.mail.internet.*;

public class ApiResponseStatus {
    private static final String[] API_URLS = {
        "https://routes.traveloes.com/",
        "https://gfs.travomint.com",
    };

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ApiResponseStatus::checkApisAndSendEmail, 0, 1, TimeUnit.HOURS);
    }

    public static void checkApisAndSendEmail() {
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("<html><body>");
        emailContent.append("<h2>API Response Status Updates</h2>");
        emailContent.append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>");
        emailContent.append("<tr><th>URL</th><th>Response Code</th><th>Status Code</th></tr>");

        for (String apiUrl : API_URLS) {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int statusCode = connection.getResponseCode();
                System.out.println("Status Code for " + apiUrl + ": " + statusCode);

                emailContent.append("<tr>")
                            .append("<td>").append(apiUrl).append("</td>")
                            .append("<td style='text-align: center;'>").append("✅").append("</td>")
                            .append("<td style='text-align: center;'>").append(statusCode).append("</td>")
                            .append("</tr>");
            } catch (IOException e) {
                emailContent.append("<tr>")
                            .append("<td>").append(apiUrl).append("</td>")
                            .append("<td style='text-align: center;'>").append("❌").append("</td>")
                            .append("<td style='color: red;'>ERROR - ").append(e.getMessage()).append("</td>")
                            .append("</tr>");
                e.printStackTrace();
            }
        }

        emailContent.append("</table>");
        emailContent.append("</body></html>");
        sendEmail(emailContent.toString());
    }

    public static void sendEmail(String emailContent) {
        final String senderEmail = "ambar.singh@snva.com";
        final String senderPassword = "lovq evli zniy iivy";
        String recipientEmail = "ambar.singh@snva.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("API Response Status Updates");
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email Sent Successfully with API Status Updates");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email. Check SMTP settings and App Password.");
        }
    }
}

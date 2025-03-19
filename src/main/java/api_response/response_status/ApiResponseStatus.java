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
        "https://routes.traveloes.com",
        "https://gfs.travomint.com",
        "https://skyroutes.travomint.com",
        "https://wegoroutes.travomint.com",
        
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
        emailContent.append("<tr><th>Serial No.</th><th>URL</th><th>Response Status</th><th>Status Code</th></tr>");

        int serialNumber = 1;
        for (String apiUrl : API_URLS) {
            String statusIcon;
            String statusCodeText;
            int statusCode;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                statusCode = connection.getResponseCode();
                System.out.println("Status Code for " + apiUrl + ": " + statusCode);

                if (statusCode == 200) {
                    statusIcon = "✅"; // OK
                } else if (statusCode >= 201 && statusCode <= 301) {
                    statusIcon = "⚠️"; // Possibly Broken
                } else if (statusCode >= 500 && statusCode <= 505) {
                    statusIcon = "⚠️"; // Internal Server Error
                } else {
                    statusIcon = "❌"; // Unknown Status
                }
                statusCodeText = String.valueOf(statusCode);
            } catch (IOException e) {
                statusIcon = "❌"; // Error icon
                statusCodeText = "Error";
                statusCode = 0;
                e.printStackTrace();
            }

            emailContent.append("<tr>")
                        .append("<td style='text-align: center;'>").append(serialNumber++).append("</td>")
                        .append("<td>").append(apiUrl).append("</td>")
                        .append("<td style='text-align: center;'>").append(statusIcon).append("</td>")
                        .append("<td style='text-align: center;'>").append(statusCodeText).append("</td>")
                        .append("</tr>");
        }

        emailContent.append("</table>");
        emailContent.append("</body></html>");
        sendEmail(emailContent.toString());
    }

    public static void sendEmail(String emailContent) {
        final String senderEmail = "ambar.singh@snva.com";
        final String senderPassword = "lovq evli zniy iivy"; // Consider using environment variables instead of hardcoding credentials.
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
            message.setSubject("API Response Status");
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email Sent Successfully with API Status Updates");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email. Check SMTP settings and App Password.");
        }
    }
}

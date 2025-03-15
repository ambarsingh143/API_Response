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
    // List of API URLs to check
    private static final String[] API_URLS = {
        "https://routes.traveloes.com/",
        "https://gfs.travomint.com",  // Example API
//        "https://api.github.com" // Add more URLs as needed
    };

    public static void main(String[] args) {
        // Schedule the task to run every hour
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ApiResponseStatus::checkApisAndSendEmail, 0, 1, TimeUnit.HOURS);
    }

    public static void checkApisAndSendEmail() {
        StringBuilder emailContent = new StringBuilder();
        emailContent.append("API Response Status Updates:\n\n");

        for (String apiUrl : API_URLS) {
            try {
                // Fetch HTTP Response Status Code
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int statusCode = connection.getResponseCode();
                System.out.println("Status Code for " + apiUrl + ": " + statusCode);

                // Append status code to email content
                emailContent.append("URL: ").append(apiUrl).append("\nStatus Code: ").append(statusCode).append("\n\n");
            } catch (IOException e) {
                emailContent.append("URL: ").append(apiUrl).append("\nStatus Code: ERROR - ").append(e.getMessage()).append("\n\n");
                e.printStackTrace();
            }
        }

        // Send email with all API statuses
        sendEmail(emailContent.toString());
    }

    public static void sendEmail(String emailContent) {
        // Sender's email credentials
        final String senderEmail = "ambar.singh@snva.com";
        final String senderPassword = "lovq evli zniy iivy";  // Use an App Password for Gmail

        // Receiver's email
        String recipientEmail = "ambar.singh@snva.com";

        // SMTP Server Properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Authenticate sender
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create Email Message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("API Response Status Updates");
            message.setText(emailContent);

            // Send Email
            Transport.send(message);
            System.out.println("✅ Email Sent Successfully with API Status Updates");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email. Check SMTP settings and App Password.");
        }
    }
}

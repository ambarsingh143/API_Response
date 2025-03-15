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
    public static void main(String[] args) {
        // Schedule the task to run every hour
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ApiResponseStatus::checkApiAndSendEmail, 0, 1, TimeUnit.HOURS);
    }

    public static void checkApiAndSendEmail() {
        try {
            // Fetch HTTP Response Status Code
            URL url = new URL("https://routes.traveloes.com/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int statusCode = connection.getResponseCode();
            System.out.println("Status Code: " + statusCode);

            // Send email with the status code
            sendEmail(statusCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendEmail(int statusCode) {
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
            message.setSubject("API Response Status Update");
            message.setText("The API response status code for https://routes.traveloes.com/ is: " + statusCode);

            // Send Email
            Transport.send(message);
            System.out.println("✅ Email Sent Successfully with Status Code: " + statusCode);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email. Check SMTP settings and App Password.");
        }
    }
}

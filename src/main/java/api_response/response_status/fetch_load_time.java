package api_response.response_status;

import org.json.JSONObject;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class fetch_load_time {

    // List of URLs to hit
    private static final String[] urls = {
            "https://routes.traveloes.com/server-stats",
            "https://skyroutes.travomint.com/server-stats",
            "https://wegoroutes.travomint.com/server-stats",
            "https://routes-sites.traveloes.com/server-stats",
            "https://googleroutes.travomint.com/server-stats"
    };

    public static void main(String[] args) {
        // Use ScheduledExecutorService to run the task every 30 minutes
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            // Fetch loadavg_1_5_15_min and load time for each URL
            StringBuilder tableContent = new StringBuilder();
            tableContent.append("<html><body><table border='1' style='border-collapse: collapse;' cellpadding='10' cellspacing='0' ><tr style= 'background:yellow'><th>URL</th><th>CPU Avg. Load</th><th>Load Time (Sec)</th></tr>");

            for (String url : urls) {
                String[] result = fetchLoadAvgAndLoadTime(url);
                String loadavg = result[0];
                String loadTime = result[1];
                tableContent.append("<tr><td>").append(url).append("</td><td style='text-align:center'>").append(loadavg).append("</td><td style='text-align:center'>").append(loadTime).append("</td></tr>");
            }

            tableContent.append("</table></body></html>");

            // Send email with the HTML report
            sendEmail(tableContent.toString());
        }, 0, 30, TimeUnit.MINUTES); // First run immediately (0), then every 30 minutes
    }

    // Fetch the loadavg_1_5_15_min value and load time from the given URL
    private static String[] fetchLoadAvgAndLoadTime(String urlString) {
        long startTime = System.currentTimeMillis(); // Start time for measuring load time
        String loadavg = "Error fetching data";
        String loadTime = "N/A";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Calculate load time (convert to seconds)
            long endTime = System.currentTimeMillis();
            loadTime = String.valueOf((endTime - startTime) / 1000.0); // Convert load time from ms to seconds

            // Parse the JSON response to get the loadavg_1_5_15_min value
            JSONObject jsonResponse = new JSONObject(response.toString());
            loadavg = jsonResponse.optString("loadavg_1_5_15_min", "Not Available");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[] { loadavg, loadTime };
    }

    // Send email with the HTML table
    private static void sendEmail(String htmlContent) {
        String fromEmail = "ambar.singh@snva.com";
        String[] toEmails = {"ambar.singh@snva.com", "alok@snva.com", "pradeep.sharma@snva.com", "davemaan@travomint.com", "arvind@snva.com", "max@travomint.com", "satya.prakash@snva.info", "vishal.nirmal@snva.info"};
        
        String host = "smtp.gmail.com"; // SMTP host (for Gmail)

        // Set email properties
        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Authenticate
        final String username = "ambar.singh@snva.com"; // Replace with your email
        final String password = "lovq evli zniy iivy"; // Replace with your email password

        // Get the Session object
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // Create the email message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));

            // Add multiple recipients
            for (String toEmail : toEmails) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            }

            message.setSubject("Server Load Average Report");

            // Set email content (HTML)
            message.setContent(htmlContent, "text/html");

            // Send the email
            Transport.send(message);
            System.out.println("Email sent successfully!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}

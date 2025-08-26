package api_response.response_status;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Scanner;

public class ApiResponseStatus {

    private static final String[] API_URLS = {
        "https://routes.traveloes.com",
        "https://skyroutes.travomint.com",
        "https://wegoroutes.travomint.com",
        "https://mca.travomint.com",
        "https://routes-sites.traveloes.com",
        "https://vbscaching.travomint.co",
        "https://vbs.travomint.com/GOOGLE/",
        "https://api.travomint.com",
        "https://testapi.traveloes.com",
        "https://staging.nobelmail.net",
        "https://cache.travomint.com",
        "https://cms.travomint.com",
        "https://payment.travomint.com",
        "http://api.traveloes.com",
        "https://gl.travomint.com",
        "https://postpapi.traveloes.com",
        "https://gl.travomint.ae",
        "https://gl.travomint.co.uk",
        "https://gl.travomint.com.au",
        "https://pay.travomint.com",
        "https://get.travomint.com",
        "https://gl.travomint.com.sg",
        "https://hotelapi.travomint.com", // Special case
        "https://ipf.travomint.com",
        "https://googleroutes.travomint.com",
        "https://stripe-payment.travomint.com/",
        "https://paytmg.travomint.com/",
        "https://us.travomint.com/",
        "https://www.travomint.in/",
        "https://www.travomint.com/",
        "https://www.travomint.com.ar/",
        "https://www.travomint.cl/",
        "https://www.travomint.com.co/",
        "https://www.travomint.co.cr/",
        "https://www.travomint.com.do/",
        "https://www.travomint.com.sv/",
        "https://www.travomint.de/",
        "https://www.travomint.com.gt/",
        "https://www.travomint.com.mx/",
        "https://www.travomint.com.pa/",
        "https://www.travomint.com.pe/",
        "https://www.travomint.com.pr/",
        "https://www.travomint.es/",
        "https://www.travomint.co.uk/",
        "https://www.travomint.ae/",
        "https://www.travomint.net/",
        "https://www.travomint.com.au/",
        "https://www.travomint.com.sg/",
        "https://www.travomint.co.nz/",
        "https://sa.travomint.net/",
        "https://www.reservationsdeal.com/",
        "https://www.fareskhalifa.com/",
        "https://www.faresclick.com/",
        "https://www.lookatfares.com/",
        "https://www.reservationsmonk.com/",
        "https://www.reservationsgate.com/",
        "https://www.quickcaribbean.com/",
        "https://www.stridetickets.com/",
        "https://www.meuseair.com/",
        "https://www.flieves.com/",
        "https://www.helpquicky.com/",
        "https://www.avtickets.com/",
        "https://www.tripscanner.com.co/",
        "https://www.pickreservations.com/",
        "https://www.allairtrip.com/",
        "https://www.webdereservadevuelos.es/",
        "https://www.myfaresadda.com/",
        "https://www.travomint.ph/",
        "https://www.travomint.com.bd/",
        "https://www.travomint.us/",
        "https://www.udantu.com/"
    };

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ApiResponseStatus::checkApisAndSendEmail, 0, 30, TimeUnit.MINUTES);
    }

    public static void checkApisAndSendEmail() {
        StringBuilder emailContent = new StringBuilder();
        StringBuilder errorEmailContent = new StringBuilder();

        emailContent.append("<html><body>");
        emailContent.append("<h2>API Response Status Updates</h2>");
        emailContent.append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>");
        emailContent.append("<tr style='background:yellow'><th>Serial No.</th><th>URL</th><th>Status Code</th><th>Load Time (ms)</th></tr>");

        errorEmailContent.append("<html><body>");
        errorEmailContent.append("<h2>🚨 Critical Issues Detected</h2>");
        errorEmailContent.append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>");
        errorEmailContent.append("<tr style='background:red; color:white;'><th>Serial No.</th><th>URL</th><th>Status</th><th>Load Time (ms)</th></tr>");

        int serialNumber = 1;
        boolean hasErrors = false;

        List<String> criticalUrls = new ArrayList<>();

        // 🔹 First pass → check all URLs
        for (String apiUrl : API_URLS) {
            ApiResult result = checkSingleApi(apiUrl);

            // Full status email
            emailContent.append("<tr>")
                    .append("<td style='text-align: center;'>").append(serialNumber++).append("</td>")
                    .append("<td>").append(apiUrl).append("</td>")
                    .append("<td style='font-weight:700;'>").append(result.statusCodeText).append("</td>")
                    .append("<td style='text-align: center;'>").append(result.loadTime == -1 ? "N/A" : result.loadTime + " ms").append("</td>")
                    .append("</tr>");

            if (result.isCritical) {
                criticalUrls.add(apiUrl);
            }
        }

        // 🔹 Wait 20 seconds before rechecking critical URLs
        if (!criticalUrls.isEmpty()) {
            try {
                Thread.sleep(20000); // 20 sec delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 🔹 Second pass → recheck only failing URLs
        int errorSerialNumber = 1;
        for (String apiUrl : criticalUrls) {
            ApiResult recheckResult = checkSingleApi(apiUrl);

            if (recheckResult.isCritical) {
                // 🔹 Extra firewall check
                boolean firewallBlocked = isFortinetBlock(apiUrl);

                if (firewallBlocked) {
                    errorEmailContent.append("<tr>")
                            .append("<td style='text-align: center;'>").append(errorSerialNumber++).append("</td>")
                            .append("<td style='font-weight:600;font-size:16px'>").append(apiUrl).append("</td>")
                            .append("<td style='color:orange; font-weight:700;text-align: center'>Firewall Blocked (Fortinet)</td>")
                            .append("<td style='text-align: center;'>N/A</td>")
                            .append("</tr>");
                    System.out.println("⚠️ " + apiUrl + " blocked by Fortinet firewall.");
                } else {
                    hasErrors = true;
                    errorEmailContent.append("<tr>")
                            .append("<td style='text-align: center;'>").append(errorSerialNumber++).append("</td>")
                            .append("<td style='font-weight:600;font-size:16px'>").append(apiUrl).append("</td>")
                            .append("<td style='color:red; font-weight:700;text-align: center'>").append(recheckResult.statusCodeText).append("</td>")
                            .append("<td style='text-align: center;'>").append(recheckResult.loadTime == -1 ? "N/A" : recheckResult.loadTime + " ms").append("</td>")
                            .append("</tr>");
                }
            } else {
                System.out.println("✅ " + apiUrl + " recovered on recheck, skipping from error mail.");
            }
        }

        emailContent.append("</table></body></html>");
        errorEmailContent.append("</table></body></html>");

        // Send full status email
        sendEmail(emailContent.toString(), "API Response Status Updates");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(dd-MM-yyyy) - (HH:mm:ss)");
        String dateTimeNow = LocalDateTime.now().format(formatter);

        // Send errors only if they persist
        if (hasErrors) {
            sendEmail(errorEmailContent.toString(),
                    "🚨 Critical! ⚠️ Attention Required 🚨 Some APIs/Websites are down 🚨 " + dateTimeNow);
        } else {
            System.out.println("✅ No persistent errors found. Skipping error email.");
        }
    }

    // 🔹 Helper Class
    static class ApiResult {
        String statusCodeText;
        int statusCode;
        long loadTime;
        boolean isCritical;

        ApiResult(String text, int code, long time, boolean critical) {
            this.statusCodeText = text;
            this.statusCode = code;
            this.loadTime = time;
            this.isCritical = critical;
        }
    }

    // 🔹 Check a single API
    public static ApiResult checkSingleApi(String apiUrl) {
        String statusCodeText;
        int statusCode;
        long loadTime;

        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            statusCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();
            loadTime = endTime - startTime;

            statusCodeText = String.valueOf(statusCode);

            // Special cases
            if (apiUrl.equals("https://hotelapi.travomint.com")) {
                if (statusCode == 502 || statusCode == 503) statusCodeText += " ❌ ALERT";
                else if (statusCode == 404) statusCodeText += " ✅ OK";
                else statusCodeText += " ✅ Normal";
            } else if (apiUrl.contains("lookatfares.com")) {
                if (statusCode == 403 || statusCode == 200) statusCodeText += " ✅ OK";
                else if (statusCode >= 500 && statusCode <= 505) statusCodeText += " ⚠️ Server Error";
                else statusCodeText += " ❌ Unexpected";
            } else {
                if (statusCode == 200) statusCodeText += " ✅ OK";
                else if (statusCode >= 500 && statusCode <= 505) statusCodeText += " ⚠️ Server Error";
                else statusCodeText += " ❌ Unknown";
            }

        } catch (IOException e) {
            statusCodeText = "Error ❌";
            statusCode = 0;
            loadTime = -1;
        }

        boolean isCritical = (statusCode == 0 || (statusCode >= 500 && statusCode <= 505));
        return new ApiResult(statusCodeText, statusCode, loadTime, isCritical);
    }

    // 🔹 Extra Fortinet Firewall Check
    public static boolean isFortinetBlock(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder responseBody = new StringBuilder();
            while (scanner.hasNextLine()) {
                responseBody.append(scanner.nextLine());
            }
            scanner.close();

            String body = responseBody.toString().toLowerCase();
            return body.contains("fortinet") || body.contains("fortigate") || body.contains("web filter");
        } catch (Exception e) {
            return false; // Could not check
        }
    }

    // 🔹 Send Email
    public static void sendEmail(String emailContent, String subject) {
        final String senderEmail = "ambar.singh@snva.com";
        final String senderPassword = "lovq evli zniy iivy"; // ⚠️ Use env var/secrets in production

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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("developer@travomint.com"));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("ambar.singh@snva.com, davemaan@travomint.com, release.management@snva.com, abhishek.mathur@snva.com, prashant@snva.com, max@travomint.com, vishal.nirmal@snva.info, satya.prakash@snva.info"));
            message.setSubject(subject);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email Sent: " + subject);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send email: " + subject);
        }
    }
}

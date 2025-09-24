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
            "https://hotelapi.travomint.com", // special case
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
            "https://www.lookatfares.com/", // special case
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
            "https://www.travomint.com.bd/",
            "https://www.travomint.us/",
            "https://www.udantu.com/",
            "https://www.travomint.fr/",
            "https://www.travomint.it/",
            "https://www.travomint.ph/",
            "https://www.travomint.com.hk/"
    };

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(ApiResponseStatus::checkApisAndSendEmail, 0, 30, TimeUnit.MINUTES);
    }

    public static void checkApisAndSendEmail() {
        Map<String, ApiResult> finalResults = new LinkedHashMap<>();
        List<String> criticalUrls = new ArrayList<>();

        // First check all URLs
        for (String apiUrl : API_URLS) {
            ApiResult result = checkSingleApi(apiUrl);
            finalResults.put(apiUrl, result);
            if (result.isCritical) {
                criticalUrls.add(apiUrl);
            }
        }

        // Retry logic for critical URLs
        for (String apiUrl : criticalUrls) {
            ApiResult original = finalResults.get(apiUrl);
            boolean persistentError = false;

            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.println("🔁 Rechecking " + apiUrl + " (Attempt " + attempt + ")");
                ApiResult retryResult = checkSingleApi(apiUrl);

                if (!retryResult.isCritical) {
                    retryResult.statusCodeText = retryResult.statusCode + " Recovered ✅";
                    finalResults.put(apiUrl, retryResult);
                    persistentError = false;
                    break;
                } else {
                    persistentError = true;
                    original = retryResult;
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (persistentError) {
                finalResults.put(apiUrl, original);
            }
        }

        // Build emails after retries
        StringBuilder emailContent = new StringBuilder();
        StringBuilder errorEmailContent = new StringBuilder();

        emailContent.append("<html><body>")
                .append("<h2>API Response Status Updates</h2>")
                .append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>")
                .append("<tr style='background:yellow'><th>S.No</th><th>URL</th><th>Status</th><th>Load Time</th></tr>");

        errorEmailContent.append("<html><body>")
                .append("<h2>🚨 Critical Issues Detected</h2>")
                .append("<table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>")
                .append("<tr style='background:red; color:white;'><th>S.No</th><th>URL</th><th>Status</th><th>Load Time</th></tr>");

        int serial = 1, errSerial = 1;
        boolean hasErrors = false;

        for (Map.Entry<String, ApiResult> entry : finalResults.entrySet()) {
            ApiResult res = entry.getValue();
            String url = entry.getKey();

            // Normal email
            String statusStyle = res.isCritical ? "color:red;" : "color:green;";
            emailContent.append("<tr>")
                    .append("<td style='text-align:center;'>").append(serial++).append("</td>")
                    .append("<td>").append(url).append("</td>")
                    .append("<td style='text-align:center;").append(statusStyle).append("'><b>")
                    .append(res.statusCodeText).append("</b></td>")
                    .append("<td style='text-align:center;'>")
                    .append(res.loadTime == -1 ? "N/A" : res.loadTime + " ms").append("</td>")
                    .append("</tr>");

            // Error email (final critical only)
            if (res.isCritical) {
                hasErrors = true;
                errorEmailContent.append("<tr>")
                        .append("<td style='text-align:center;'>").append(errSerial++).append("</td>")
                        .append("<td style='font-size:16px'><b>").append(url).append("</b></td>")
                        .append("<td style='text-align:center; color:red;font-size:15px'><b>")
                        .append(res.statusCodeText).append("</b></td>")
                        .append("<td style='text-align:center;'>")
                        .append(res.loadTime == -1 ? "N/A" : res.loadTime + " ms").append("</td>")
                        .append("</tr>");
            }
        }

        emailContent.append("</table></body></html>");
        errorEmailContent.append("</table></body></html>");

        // Send normal mail
        sendEmail(emailContent.toString(), "API Response Status Updates");

        // Send error mail only if errors left
        if (hasErrors) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("(dd-MM-yyyy) - (HH:mm:ss)");
            String dateTimeNow = LocalDateTime.now().format(formatter);

            sendEmail(errorEmailContent.toString(),
                    "🚨 Critical! ⚠️ Attention Required 🚨 Some APIs/Websites are down 🚨 " + dateTimeNow);
        } else {
            System.out.println("✅ No persistent errors found. Skipping error email.");
        }
    }

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

    // ✅ Main Logic for Checking APIs
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

            if (apiUrl.equals("https://hotelapi.travomint.com")) {
                if (statusCode == 200 || statusCode == 404) {
                    statusCodeText += " OK ✅";
                } else {
                    statusCodeText += " ❌ ALERT";
                }
            } else if (apiUrl.contains("lookatfares.com")) {
                if (statusCode == 200 || statusCode == 403) {
                    statusCodeText += " OK ✅";
                } else {
                    statusCodeText += " ❌ Unexpected";
                }
            } else {
                if (statusCode == 200) {
                    statusCodeText += " OK ✅";
                } else {
                    statusCodeText += " ❌ Not OK";
                }
            }

        } catch (IOException e) {
            statusCodeText = "Error ❌";
            statusCode = 0;
            loadTime = -1;
        }

        boolean isCritical;

        if (apiUrl.equals("https://hotelapi.travomint.com")) {
            isCritical = !(statusCode == 200 || statusCode == 404);
        } else if (apiUrl.contains("lookatfares.com")) {
            isCritical = !(statusCode == 200 || statusCode == 403);
        } else {
            isCritical = (statusCode != 200);
        }

        return new ApiResult(statusCodeText, statusCode, loadTime, isCritical);
    }

    public static void sendEmail(String emailContent, String subject) {
        final String senderEmail = "ambar.singh@snva.com";
        final String senderPassword = "lovq evli zniy iivy"; // ⚠️ In production use env variable

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
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse("ambar.singh@snva.com, davemaan@travomint.com, release.management@snva.com, abhishek.mathur@snva.com, max@travomint.com, vishal.nirmal@snva.info, satya.prakash@snva.info"));
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

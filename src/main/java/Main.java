import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        IOUtils ioUtils = new IOUtils();
        try {
            String mails = FileUtils.readFileToString(new File("tickets.mbox"));
//            Remove all line breaks
            mails = mails.replaceAll("=\\r\\n", "");
//            Get all urls
            Pattern urlPattern = Pattern.compile("https://www\\.sbb\\.ch/en/buying/pages/bestellung/bestellungUebersicht\\.xhtml\\?id=(?:[\\w-])+");
            Matcher matcher = urlPattern.matcher(mails);
            while (matcher.find()) {
                String urlString = matcher.group();
//                Parse the page
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);
                Connection.Response response = Jsoup.connect(urlString).method(Connection.Method.GET).execute();
                Document ticketPage = response.parse();
                String orderDateString = ticketPage.select("dd:first-of-type").first().text();
                String traveler = ticketPage.select(".var_order_reisender>span:first-child").first().text();
                traveler = removeString(traveler, " ");
                String route = ticketPage.select(".mod_breadcrumb_button_title[itemprop=name]").first().text();
                route = removeString(route, " ", ",");
                SimpleDateFormat sourceFormat = new SimpleDateFormat("E, d.M.y");
                Date orderDate = sourceFormat.parse(orderDateString);
                String price = ticketPage.select(".mod_confirmation_total_price_value").first().text();
                price = removeString(price, " ");
//                Download and write the pdf
                URL pdfUrl = new URL("https://www.sbb.ch/en/buying/beleg/billett-email/.pdf");
                IOUtils.toByteArray(pdfUrl.openStream());
                SimpleDateFormat targetFormat = new SimpleDateFormat("yy-MM-dd");
                String pdfName = String.format("SBB-Ticket_%s_%s-%s-%s.pdf", targetFormat.format(orderDate), traveler, route, price);
                File pdfFile = new File(pdfName);
                FileOutputStream outputStream = new FileOutputStream(pdfFile);
                outputStream.write(IOUtils.toByteArray(pdfUrl.openStream()));
                outputStream.close();
            }
        } catch (IOException|ParseException e) {
            e.printStackTrace();
        }
    }

    private static String removeString(String subject, String... removables) {
        String result = subject;
        for (String character : removables) {
             result = result.replace(character, "");
        }
        return result;
    }

}

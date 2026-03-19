package dg.swiss.swiss_dg_db.scrape;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class JsoupConnectionFactory {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    private static final String REFERRER = "https://www.pdga.com";

    public static Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer(REFERRER)
                .header(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br")
                .timeout(10_000);
    }
}

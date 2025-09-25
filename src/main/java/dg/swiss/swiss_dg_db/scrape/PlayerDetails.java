package dg.swiss.swiss_dg_db.scrape;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Setter
@Getter
public class PlayerDetails {
    private static final String baseUrl = "https://www.pdga.com/player/";
    private String firstname;
    private String lastname;
    private long pdgaNumber;
    private long sdaNumber;
    private Boolean swisstourLicense;
    private Boolean isPro;

    public void scrapePlayerInfo(long pdgaNumber) throws IOException {
        this.pdgaNumber = pdgaNumber;
        // get the DOM of the player
        Document document = Jsoup.connect(baseUrl + pdgaNumber).get();
        // call function to scrape names and assign
        this.firstname = scrapeName(document).getFirstName();
        this.lastname = scrapeName(document).getLastName();
        this.isPro = scrapePro(document);
    }

    private NameConverter.NameInfo scrapeName(Document document) {
        Element nameAndPdgaNumber = document.selectFirst("div.pane-page-title h1");
        if (nameAndPdgaNumber != null) {
            String[] names = nameAndPdgaNumber.text().split("#")[0].trim().split("\\s+");
            return NameConverter.splitName(names);
        } else {
            return new NameConverter.NameInfo(null, null);
        }
    }

    private Boolean scrapePro(Document document) {
        Element classificationElement = document.selectFirst("ul.player-info.info-list li.classification");
        if (classificationElement != null) {
            String classification = classificationElement.text().replace("Classification: ", "").trim();
            return classification.equals("Professional");
        }
        return false;

    }
}

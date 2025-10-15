package dg.swiss.swiss_dg_db.scrape;

import dg.swiss.swiss_dg_db.exceptions.EventDoesNotExistException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLOutput;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@Setter
@Getter
public class EventDetails {
    private static final String baseUrl = "https://www.pdga.com/tour/event/";
    private String name;
    private LocalDate date;
    private int numberDays;
    private String tier;
    private String city;
    private String country;
    private Integer numberPlayers;
    private Double points;
    private Double purse;
    private List<TournamentDetail> tournaments;

    public void scrapeEventInfo(Long eventId) throws IOException {
        String url = baseUrl + eventId;
        // Throw exception if url returns 404
        try {
            Connection.Response response = Jsoup.connect(url).execute();
            // Continue processing if no exception
        } catch (org.jsoup.HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                throw new EventDoesNotExistException();
            } else {
                throw e;
            }
        }
        // get the DOM of the event
        Document document = Jsoup.connect(url).get();
        // scrape for event name and set it
        String name = scrapeName(document);
        this.setName(name);

        // scrape for event info
        Element eventInfo = document.selectFirst("div.panel-pane.pane-tournament-event-info");
        if (eventInfo != null) {
            DateConverter.DateInfo dateInfo = scrapeDateInfo(eventInfo);
            this.setDate(dateInfo.getStartDate());
            this.setNumberDays(dateInfo.getDays());
            LocationConverter.LocationInfo locationInfo = scrapeLocationInfo(eventInfo);
            this.setCity(locationInfo.getCity());
            this.setCountry(locationInfo.getCountry());
            this.setTier(scrapeTier(eventInfo));
        } else {
            this.setDate(LocalDate.EPOCH);
            this.setNumberDays(0);
            this.setTier("Event info not found");
        }

        // scrape for event number of players and purse
        this.setNumberPlayers(scrapeNumberPlayers(document));
        this.setPurse(scrapePurse(document));
    }

    private String scrapeName(Document document) {
        Element name = document.selectFirst("div.panel-pane.pane-page-title h1");
        if (name != null) {
            return name.text().trim();
        } else {
            return "Event name not found";
        }
    }

    private DateConverter.DateInfo scrapeDateInfo(Element eventInfo) {
        Element dateElement = eventInfo.selectFirst("li.tournament-date");
        if (dateElement != null) {
            String dateText = dateElement.text().replace("Date: ", "").trim();
            try {
                return DateConverter.convertDate(dateText);
            } catch (DateTimeParseException e) {
                throw e;
            }
        } else {
            return null;
        }
    }

    public LocationConverter.LocationInfo scrapeLocationInfo(Element eventInfo) {
        Element locationElement = eventInfo.selectFirst("li.tournament-location");
        if (locationElement != null) {
            String locationText = locationElement.text().replace("Location: ", "").trim();
            return LocationConverter.convertLocation(locationText);
        } else {
            return new LocationConverter.LocationInfo(null, null, null);
        }
    }

    private String scrapeTier(Element eventInfo) {
        Element tierElement = eventInfo.selectFirst("div.pane-content h4");
        if (tierElement != null) {
            return tierElement.text().trim();
        } else {
            return "Event tier not found";
        }
    }

    private Integer scrapeNumberPlayers(Document document) {
        Element numberPlayersElement = document.selectFirst("td.players");
        if (numberPlayersElement != null) {
            return Integer.parseInt(numberPlayersElement.text().trim());
        } else {
            return 0;
        }
    }

    private Double scrapePurse(Document document) {
        Element purseElement = document.selectFirst("td.purse");
        if (purseElement != null && !purseElement.text().equals("Projected payout")) {
            return Double.parseDouble(purseElement.text().replace("$", "").replace(",", "").trim());
        } else {
            return 0.00;
        }
    }

    public void scrapeEventResults(Long eventId, Integer points) throws IOException {
        // get the DOM of the event
        Document document = Jsoup.connect(baseUrl + eventId).get();
        // scrape for the tournaments
        this.tournaments = scrapeTournaments(document, points);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class TournamentDetail {
        private String name;
        private Long pdgaNumber;
        private String division;
        private Integer score;
        private Integer place;
        private Double prize;
        private Double points;
        private List<RoundDetail> rounds;
    }

    @AllArgsConstructor
    @Getter
    public static class RoundDetail {
        private Integer roundNumber;
        private Integer score;
        private Integer rating;
    }

    public static List<TournamentDetail> scrapeTournaments(Document document, Integer points) {
        Element results = document.selectFirst(".leaderboard");
        List<TournamentDetail> tournamentDetails = new ArrayList<>();

        assert results != null;
        Elements categories = results.select("details");
        for (Element c : categories) {
            Element divisionElement = c.selectFirst(".division");
            if (divisionElement != null) {
                String division = divisionElement.text().split(" ")[0];

                Elements tournamentsOdd = c.select(".odd");
                Elements tournamentsEven = c.select(".even");

                List<Element> tournaments = new ArrayList<>();
                tournaments.addAll(tournamentsOdd);
                tournaments.addAll(tournamentsEven);

                // Add points
                HashMap<Integer, Double> pts = createPtsDict(points, tournamentDetails.size());

                for (Element tournament : tournaments) {
                    TournamentDetail tournamentDetail = parseTournament(tournament, division);
                    if (tournamentDetail != null) {
                        tournamentDetail.setPoints(pts.get(tournamentDetail.getPlace()));
                        tournamentDetails.add(tournamentDetail);
                    }
                }
            }
        }
        return tournamentDetails;
    }

    private static HashMap<Integer, Double> createPtsDict(double scale, Integer noPlayers) {
        HashMap<Integer, Double> pointsMap = new HashMap<>();
        // Base points defined within this method
        int[] basePoints = {100, 90, 81, 73, 66, 60, 55, 50, 46, 42,
                38, 35, 32, 29, 26, 23, 21, 19,
                17, 15, 13, 11, 10, 9, 8, 7, 6, 5};
        for (int i = 0; i < basePoints.length; i++) {
            // Multiply by scale factor based on input
            pointsMap.put(i + 1, basePoints[i] * (scale / 100));
        }
        // Determine the minimum points for additional players
        double minPoints = 5 * (scale / 100);
        // Populate points for players above 28
        for (int i = 29; i <= noPlayers; i++) {
            pointsMap.put(i, minPoints); // Assign minimum points
        }
        return pointsMap;
    }

    private static TournamentDetail parseTournament(Element tournament, String division) {
        try {
            Element placeElement = tournament.selectFirst(".place");
            String placeText;
            if (placeElement != null) {
                placeText = placeElement.text().trim();
            } else {
                placeText = "";
            }
            Integer place;
            if (!placeText.isEmpty()) {
                place = Integer.parseInt(placeText);
            } else {
                place = null;
            }

            String name = tournament.select(".player").text();
            String pdgaNumberString = tournament.select(".pdga-number").text();
            Long pdgaNumber;
            if (!pdgaNumberString.isEmpty()) {
                pdgaNumber = Long.parseLong(pdgaNumberString);
            } else {
                pdgaNumber = null;
            }
            String scoreString = tournament.select(".total").text();
            Integer score;
            if (!scoreString.equals("DNF") && !scoreString.isEmpty()) {
                score = Integer.parseInt(scoreString);
            } else {
                score = null;
            }
            String prizeString = tournament.select(".prize").text();
            Double prize;
            if (!prizeString.isEmpty()) {
                prize = Double.parseDouble(prizeString.replace("$", "").replace(",", "").trim());
            } else {
                prize = null;
            }
            // Scrape rounds
            Elements roundScores = tournament.select("td.round > a.score");
            Elements roundRatings = tournament.select("td.round-rating");

            List<RoundDetail> rounds = new ArrayList<>();
            for (int i = 0; i < roundScores.size(); i++) {
                Integer roundScore = null;
                Integer roundRating = null;
                try {
                    String scoreText = roundScores.get(i).text().trim();
                    if (!scoreText.isEmpty()) {
                        roundScore = Integer.parseInt(scoreText);
                    }
                } catch (Exception ignored) {}
                try {
                    String ratingText = roundRatings.get(i).text().trim();
                    if (!ratingText.isEmpty()) {
                        roundRating = Integer.parseInt(ratingText);
                    }
                } catch (Exception ignored) {}
                rounds.add(new RoundDetail(i+1, roundScore, roundRating));
            }

            return new TournamentDetail(name, pdgaNumber, division, score, place, prize, null,rounds);
        } catch (Exception e) {
            return null;
        }
    }
}

package dg.swiss.swiss_dg_db.pdga_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PdgaApiPlayerService {

    private final String API_URL;
    private final PdgaApiSessionService pdgaApiSessionService;
    private final RestClient restClient;

    Logger logger = LoggerFactory.getLogger(PdgaApiPlayerService.class);

    public PdgaApiPlayerService(
            PdgaApiSessionService pdgaApiSessionService,
            RestClient.Builder restClientBuilder,
            @Value("${app.pdga-api.url}") String apiUrl) {
        this.pdgaApiSessionService = pdgaApiSessionService;
        this.API_URL = apiUrl;
        if (API_URL == null) {
            throw new IllegalArgumentException("PDGA API URL must be provided in application.yml");
        }
        this.restClient = restClientBuilder.baseUrl(API_URL).build();
    }

    public PdgaApiPlayerResponseDTO getPlayerInfo(long pdgaNumber) {
        if (!pdgaApiSessionService.isSessionActive()) {
            pdgaApiSessionService.startSession();
        }
        PdgaApiPlayersResponseDTO playerInfo =
                restClient
                        .get()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path("player-statistics")
                                                .queryParam(
                                                        "pdga_number", String.valueOf(pdgaNumber))
                                                .queryParam("year", "2026")
                                                .build())
                        .header(
                                HttpHeaders.COOKIE,
                                pdgaApiSessionService.getSessionName()
                                        + "="
                                        + pdgaApiSessionService.getSessionId())
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(PdgaApiPlayersResponseDTO.class);
        // Implement the logic to get player statistics using the sessionId and token
        if (playerInfo == null
                || playerInfo.getPlayers() == null
                || playerInfo.getPlayers().isEmpty()) {
            logger.error("Failed to retrieve player information for PDGA number: " + pdgaNumber);
            throw new RuntimeException(
                    "Failed to retrieve player information for PDGA number: " + pdgaNumber);
        }
        return playerInfo.getPlayers().getFirst();
    }
}

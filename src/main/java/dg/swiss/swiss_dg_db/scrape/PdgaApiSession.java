package dg.swiss.swiss_dg_db.scrape;

import com.fasterxml.jackson.annotation.JsonProperty;
import dg.swiss.swiss_dg_db.player.PdgaApiPlayerDTO;
import dg.swiss.swiss_dg_db.player.PdgaApiPlayersResponseDTO;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Getter
public class PdgaApiSession {
    private static final String API_URL = "https://api.pdga.com/services/json/";
    private final RestClient restClient;
    private final String pdgaUsername;
    private final String pdgaPassword;
    private String sessionId;
    private String sessionName;
    private String token;
    private int status;

    public PdgaApiSession(
            @Value("${app.pdga-api.username}") String pdgaUsername,
            @Value("${app.pdga-api.password}") String pdgaPassword,
            RestClient.Builder restClientBuilder) {
        this.pdgaUsername = pdgaUsername;
        this.pdgaPassword = pdgaPassword;
        this.restClient = restClientBuilder.baseUrl(API_URL).build();
    }

    public PdgaApiPlayerDTO getPlayerInfo(long pdgaNumber) {
        if (!isSessionActive()) {
            startSession();
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
                        .header(HttpHeaders.COOKIE, this.sessionName + "=" + this.sessionId)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(PdgaApiPlayersResponseDTO.class);
        // Implement the logic to get player statistics using the sessionId and token
        if (playerInfo == null
                || playerInfo.getPlayers() == null
                || playerInfo.getPlayers().isEmpty()) {
            throw new RuntimeException(
                    "Failed to retrieve player information for PDGA number: " + pdgaNumber);
        }
        return playerInfo.getPlayers().getFirst();
    }

    private void startSession() {
        LoginResponse loginResponse =
                this.restClient
                        .post()
                        .uri("user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(new LoginRequest(pdgaUsername, pdgaPassword))
                        .retrieve()
                        .body(LoginResponse.class);

        if (loginResponse == null || loginResponse.sessionId() == null) {
            throw new RuntimeException("Failed to start PDGA API session");
        }
        this.sessionId = loginResponse.sessionId();
        this.sessionName = loginResponse.sessionName();
        this.token = loginResponse.token();
    }

    private boolean isSessionActive() {
        return this.sessionId != null && !this.sessionId.isEmpty();
    }

    private record LoginRequest(
            @JsonProperty("username") String username, @JsonProperty("password") String password) {}

    private record LoginResponse(
            @JsonProperty("sessid") String sessionId,
            @JsonProperty("token") String token,
            @JsonProperty("session_name") String sessionName) {}
}

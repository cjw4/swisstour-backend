package dg.swiss.swiss_dg_db.pdga_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import dg.swiss.swiss_dg_db.player.PlayerService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Getter
public class PdgaApiSessionService {
    private final String API_URL;
    private final RestClient restClient;
    private final String pdgaUsername;
    private final String pdgaPassword;
    private String sessionId;
    private String sessionName;
    private String token;

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public PdgaApiSessionService(
            @Value("${app.pdga-api.url}") String apiUrl,
            @Value("${app.pdga-api.username}") String pdgaUsername,
            @Value("${app.pdga-api.password}") String pdgaPassword,
            RestClient.Builder restClientBuilder) {
        if (pdgaUsername == null || pdgaPassword == null || apiUrl == null) {
            throw new IllegalArgumentException(
                    "PDGA API username, password, and URL must be provided in application.yml");
        }
        this.pdgaUsername = pdgaUsername;
        this.pdgaPassword = pdgaPassword;
        this.API_URL = apiUrl;
        this.restClient = restClientBuilder.baseUrl(API_URL).build();
    }

    protected void startSession() {
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
            logger.error("Failed to start PDGA API session");
            throw new RuntimeException("Failed to start PDGA API session");
        }
        this.sessionId = loginResponse.sessionId();
        this.sessionName = loginResponse.sessionName();
        this.token = loginResponse.token();
    }

    protected void endSession() {
        ResponseEntity<Void> response =
                this.restClient
                        .post()
                        .uri("user/logout")
                        .header(HttpHeaders.COOKIE, this.sessionName + "=" + this.sessionId)
                        .header("X-CSRF-Token", this.token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .toBodilessEntity();

        if (response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
            this.sessionId = null;
            this.sessionName = null;
            this.token = null;
        } else {
            logger.warn("Failed to end PDGA API session");
        }
    }

    protected boolean isSessionActive() {
        if (this.sessionId != null
                && !this.sessionId.isEmpty()
                && this.token != null
                && !this.token.isEmpty()
                && this.sessionName != null
                && !this.sessionName.isEmpty()) {
            return getConnectionStatus();
        }
        return false;
    }

    private boolean getConnectionStatus() {
        ConnectionStatusResponse connectionStatus =
                this.restClient
                        .post()
                        .uri("system/connect")
                        .header(
                                "Cookie",
                                this.sessionName + "=" + this.sessionId,
                                "X-CSRF-Token",
                                this.token)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(ConnectionStatusResponse.class);
        if (connectionStatus != null && connectionStatus.user.status == "1") {
            return true;
        }
        return false;
    }

    private record LoginRequest(String username, String password) {}

    private record LoginResponse(
            @JsonProperty("sessid") String sessionId,
            @JsonProperty("token") String token,
            @JsonProperty("session_name") String sessionName) {}

    private record ConnectionStatusResponse(
            @JsonProperty("sessid") String sessionId,
            @JsonProperty("session_name") String sessionName,
            UserResponse user) {}

    private record UserResponse(String name, String mail, Long login, String status) {}
}

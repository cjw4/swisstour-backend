package dg.swiss.swiss_dg_db.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dg.swiss.swiss_dg_db.scrape.PlayerDetails;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private PlayerDetails playerDetails;
    @Mock private TournamentRepository tournamentRepository;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService =
                spy(
                        new PlayerService(
                                playerRepository, publisher, playerDetails, tournamentRepository));
    }

    // -------------------------------------------------------------------------
    // parseGoogleSheetCsv
    // -------------------------------------------------------------------------

    @Test
    void parseGoogleSheetCsv_parsesStandardRow() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        PlayerDTO player = result.get(0);
        assertThat(player.getSdaNumber()).isEqualTo(1001L);
        assertThat(player.getPdgaNumber()).isEqualTo(27523L);
        assertThat(player.getFirstname()).isEqualTo("Paul");
        assertThat(player.getLastname()).isEqualTo("McBeth");
        assertThat(player.getSwisstourLicense()).isTrue();
    }

    @Test
    void parseGoogleSheetCsv_sdaLettersStripped() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nABC1001XY,27523,McBeth,Paul\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
    }

    @Test
    void parseGoogleSheetCsv_emptyPdgaNumber_setsNull() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,,McBeth,Paul\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPdgaNumber()).isNull();
    }

    @Test
    void parseGoogleSheetCsv_malformedRow_isSkipped() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).isEmpty();
    }

    @Test
    void parseGoogleSheetCsv_blankRow_stopsReading() throws IOException {
        String csv =
                "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\n\nSDA1002,12345,Doe,Jane\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
    }

    @Test
    void parseGoogleSheetCsv_multipleValidRows_parsedAll() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\nSDA1002,,Doe,Jane\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = playerService.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
        assertThat(result.get(1).getSdaNumber()).isEqualTo(1002L);
    }

    // -------------------------------------------------------------------------
    // addPlayersFromGoogleSheet
    // -------------------------------------------------------------------------

    @Test
    void addPlayersFromGoogleSheet_matchBySda_setsLicenseTrue()
            throws IOException, InterruptedException {
        // Arrange
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(false)
                        .build();

        doReturn(List.of(sheetPlayer)).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();
        stubUpdate(1L);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSwisstourLicense()).isTrue();
    }

    @Test
    void addPlayersFromGoogleSheet_matchByPdga_setsLicenseTrueAndUpdatesSdaNumber()
            throws IOException, InterruptedException {
        // Arrange
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(2001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(null)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(false)
                        .build();

        doReturn(List.of(sheetPlayer)).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();
        stubUpdate(1L);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSwisstourLicense()).isTrue();
        assertThat(result.get(0).getSdaNumber()).isEqualTo(2001L);
    }

    @Test
    void addPlayersFromGoogleSheet_matchByName_setsLicenseTrueAndUpdatesSdaNumber()
            throws IOException, InterruptedException {
        // Arrange
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(3001L)
                        .pdgaNumber(null)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(null)
                        .pdgaNumber(null)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(false)
                        .build();

        doReturn(List.of(sheetPlayer)).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();
        stubUpdate(1L);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSwisstourLicense()).isTrue();
        assertThat(result.get(0).getSdaNumber()).isEqualTo(3001L);
    }

    @Test
    void addPlayersFromGoogleSheet_noMatch_setsLicenseFalse()
            throws IOException, InterruptedException {
        // Arrange - empty sheet means no DB player can match
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();

        doReturn(List.of()).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();
        stubUpdate(1L);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSwisstourLicense()).isFalse();
    }

    @Test
    void addPlayersFromGoogleSheet_sdaMatchWithNoPdgaInSheet_updatesName()
            throws IOException, InterruptedException {
        // Arrange - sheet player has no PDGA, so name should be synced to DB player
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(1001L)
                        .pdgaNumber(null)
                        .firstname("Paolo")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(1001L)
                        .pdgaNumber(null)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(false)
                        .build();

        doReturn(List.of(sheetPlayer)).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();
        stubUpdate(1L);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstname()).isEqualTo("Paolo");
    }

    @Test
    void addPlayersFromGoogleSheet_sheetPlayerNotInDb_isCreated()
            throws IOException, InterruptedException {
        // Arrange
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();

        doReturn(List.of(sheetPlayer)).when(playerService).readGoogleSheet();
        doReturn(List.of()).when(playerService).findAll();

        Player savedPlayer =
                Player.builder()
                        .id(1L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .swisstourLicense(true)
                        .isPro(false)
                        .build();
        when(playerRepository.save(any())).thenReturn(savedPlayer);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(savedPlayer));
        when(playerDetails.getFirstname()).thenReturn("Paul");
        when(playerDetails.getLastname()).thenReturn("McBeth");
        when(playerDetails.getIsPro()).thenReturn(false);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        verify(playerDetails).scrapePlayerInfo(27523L);
        verify(playerRepository).save(any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
    }

    @Test
    void addPlayersFromGoogleSheet_returnsAllUpdatedAndCreatedPlayers()
            throws IOException, InterruptedException {
        // Arrange - one DB player matched, one sheet player is new
        PlayerDTO sheetPlayerMatched =
                PlayerDTO.builder()
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO sheetPlayerNew =
                PlayerDTO.builder()
                        .sdaNumber(2001L)
                        .pdgaNumber(null)
                        .firstname("Jane")
                        .lastname("Doe")
                        .swisstourLicense(true)
                        .build();
        PlayerDTO dbPlayer =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .swisstourLicense(false)
                        .build();

        doReturn(List.of(sheetPlayerMatched, sheetPlayerNew)).when(playerService).readGoogleSheet();
        doReturn(List.of(dbPlayer)).when(playerService).findAll();

        Player existingEntity =
                Player.builder()
                        .id(1L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .swisstourLicense(false)
                        .isPro(false)
                        .build();
        Player newEntity =
                Player.builder()
                        .id(2L)
                        .firstname("Jane")
                        .lastname("Doe")
                        .sdaNumber(2001L)
                        .swisstourLicense(true)
                        .isPro(false)
                        .build();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(newEntity));
        when(playerRepository.save(any())).thenReturn(existingEntity).thenReturn(newEntity);

        // Act
        List<PlayerDTO> result = playerService.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void stubUpdate(Long id) {
        Player entity = Player.builder().id(id).swisstourLicense(false).isPro(false).build();
        when(playerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(playerRepository.save(any())).thenReturn(entity);
    }
}

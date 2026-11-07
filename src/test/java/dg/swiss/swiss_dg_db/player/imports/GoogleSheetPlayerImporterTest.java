package dg.swiss.swiss_dg_db.player.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dg.swiss.swiss_dg_db.player.PlayerDTO;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.player.exceptions.GoogleSheetUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoogleSheetPlayerImporterTest {

    @Mock private PlayerService playerService;

    private GoogleSheetPlayerImporter importer;

    @BeforeEach
    void setUp() {
        importer = spy(new GoogleSheetPlayerImporter(playerService));
    }

    // -------------------------------------------------------------------------
    // readGoogleSheet
    // -------------------------------------------------------------------------

    @Test
    void readGoogleSheet_inaccessibleUrl_throwsGoogleSheetUnavailableException() {
        ReflectionTestUtils.setField(importer, "csvExportUrl", "not-a-valid-url");

        assertThatThrownBy(() -> importer.readGoogleSheet())
                .isInstanceOf(GoogleSheetUnavailableException.class)
                .hasMessageContaining("not-a-valid-url")
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    // -------------------------------------------------------------------------
    // parseGoogleSheetCsv
    // -------------------------------------------------------------------------

    @Test
    void parseGoogleSheetCsv_parsesStandardRow() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

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

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
    }

    @Test
    void parseGoogleSheetCsv_emptyPdgaNumber_setsNull() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,,McBeth,Paul\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPdgaNumber()).isNull();
    }

    @Test
    void parseGoogleSheetCsv_malformedRow_isSkipped() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

        assertThat(result).isEmpty();
    }

    @Test
    void parseGoogleSheetCsv_blankRow_stopsReading() throws IOException {
        String csv =
                "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\n\nSDA1002,12345,Doe,Jane\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(1001L);
    }

    @Test
    void parseGoogleSheetCsv_multipleValidRows_parsedAll() throws IOException {
        String csv = "SDA,PDGA,Nachname,Vorname\nSDA1001,27523,McBeth,Paul\nSDA1002,,Doe,Jane\n";
        BufferedReader reader = new BufferedReader(new StringReader(csv));

        List<PlayerDTO> result = importer.parseGoogleSheetCsv(reader);

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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSwisstourLicense()).isTrue();
        assertThat(result.get(0).getSdaNumber()).isEqualTo(3001L);
    }

    @Test
    void addPlayersFromGoogleSheet_dbPlayerHasPdgaSheetHasNone_matchByNameAddsSda()
            throws IOException, InterruptedException {
        PlayerDTO sheetPlayer =
                PlayerDTO.builder()
                        .sdaNumber(4001L)
                        .pdgaNumber(null)
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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSdaNumber()).isEqualTo(4001L);
        assertThat(result.get(0).getSwisstourLicense()).isTrue();
        verify(playerService, never()).create(any());
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

        doReturn(List.of()).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

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

        doReturn(List.of(sheetPlayer)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of());

        PlayerDTO withDetails =
                PlayerDTO.builder()
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .isPro(false)
                        .build();
        PlayerDTO created =
                PlayerDTO.builder()
                        .id(1L)
                        .sdaNumber(1001L)
                        .pdgaNumber(27523L)
                        .firstname("Paul")
                        .lastname("McBeth")
                        .isPro(false)
                        .build();
        when(playerService.addDetails(sheetPlayer)).thenReturn(withDetails);
        when(playerService.create(withDetails)).thenReturn(1L);
        when(playerService.get(1L)).thenReturn(created);

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

        // Assert
        verify(playerService).create(withDetails);
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

        doReturn(List.of(sheetPlayerMatched, sheetPlayerNew)).when(importer).readGoogleSheet();
        when(playerService.findAll()).thenReturn(List.of(dbPlayer));

        when(playerService.addDetails(sheetPlayerNew)).thenReturn(sheetPlayerNew);
        when(playerService.create(sheetPlayerNew)).thenReturn(2L);
        when(playerService.get(2L)).thenReturn(sheetPlayerNew);

        // Act
        List<PlayerDTO> result = importer.addPlayersFromGoogleSheet();

        // Assert
        assertThat(result).hasSize(2);
    }
}

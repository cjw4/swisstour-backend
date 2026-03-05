package dg.swiss.swiss_dg_db.player;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class PlayerRepositoryTests {

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void PlayerRepository_existsByPdgaNumber_ReturnsTrue() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .pdgaNumber(27523L)
                .swisstourLicense(false)
                .isPro(true)
                .build();

        // Act
        playerRepository.save(player);

        // Assert
        Assertions.assertThat(playerRepository.existsByPdgaNumber(player.getPdgaNumber())).isTrue();
    }

    @Test
    void PlayerRepository_existsBySdaNumber_ReturnsTrue() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .sdaNumber(1001L)
                .swisstourLicense(false)
                .isPro(true)
                .build();

        // Act
        playerRepository.save(player);

        // Assert
        Assertions.assertThat(playerRepository.existsBySdaNumber(player.getSdaNumber())).isTrue();
    }

    @Test
    void PlayerRepository_existsByFirstnameAndLastname_ReturnsTrue() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .swisstourLicense(false)
                .isPro(true)
                .build();

        // Act
        playerRepository.save(player);

        // Assert
        Assertions.assertThat(playerRepository.existsByFirstnameAndLastname("Paul", "McBeth")).isTrue();
    }

    @Test
    void PlayerRepository_findByPdgaNumber_ReturnsPlayer() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .pdgaNumber(27523L)
                .swisstourLicense(false)
                .isPro(true)
                .build();

        // Act
        playerRepository.save(player);

        // Assert
        Assertions.assertThat(playerRepository.findByPdgaNumber(27523L)).isPresent();
    }

    @Test
    void PlayerRepository_findByFirstnameAndLastname_ReturnsPlayer() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .swisstourLicense(false)
                .isPro(true)
                .build();

        // Act
        playerRepository.save(player);

        // Assert
        Assertions.assertThat(playerRepository.findByFirstnameAndLastname("Paul", "McBeth")).isPresent();
    }
}

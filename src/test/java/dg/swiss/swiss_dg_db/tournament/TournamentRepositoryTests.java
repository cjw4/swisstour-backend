package dg.swiss.swiss_dg_db.tournament;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.EventRepository;
import dg.swiss.swiss_dg_db.player.Player;
import dg.swiss.swiss_dg_db.player.PlayerRepository;
import dg.swiss.swiss_dg_db.round.Round;
import dg.swiss.swiss_dg_db.round.RoundRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class TournamentRepositoryTests {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void TournamentRepository_findTournamentsWithEventAndRoundsByPlayerId_ReturnTournaments() {
        // Arrange
        Player player = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .swisstourLicense(true)
                .isPro(true)
                .build();
        playerRepository.save(player);

        Player otherPlayer = Player.builder()
                .firstname("Ricky")
                .lastname("Wysocki")
                .swisstourLicense(true)
                .isPro(true)
                .build();
        playerRepository.save(otherPlayer);

        Event event = Event.builder()
                .year(2025)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .build();
        eventRepository.save(event);

        Tournament tournament = Tournament.builder()
                .division("MPO")
                .place(1)
                .player(player)
                .event(event)
                .build();
        tournamentRepository.save(tournament);

        Tournament otherTournament = Tournament.builder()
                .division("MPO")
                .place(2)
                .player(otherPlayer)
                .event(event)
                .build();
        tournamentRepository.save(otherTournament);

        Round round = Round.builder()
                .roundNumber(1)
                .rating(1000)
                .score(54)
                .tournament(otherTournament)
                .build();
        roundRepository.save(round);

        entityManager.flush();
        entityManager.clear();

        // Act
        List<Tournament> result = tournamentRepository.findTournamentsWithEventAndRoundsByPlayerId(otherPlayer.getId());

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.getFirst().getPlayer().getId()).isEqualTo(otherPlayer.getId());
        Assertions.assertThat(result.getFirst().getEvent().getId()).isEqualTo(event.getId());
        Assertions.assertThat(result.getFirst().getRounds()).hasSize(1);
    }

    @Test
    void TournamentRepository_findTournamentsByDivision_ReturnTournaments() {
        // Arrange
        Player mpoPlayer = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .swisstourLicense(true)
                .isPro(true)
                .build();
        playerRepository.save(mpoPlayer);

        Player fpoPlayer = Player.builder()
                .firstname("Paige")
                .lastname("Pierce")
                .swisstourLicense(true)
                .isPro(true)
                .build();
        playerRepository.save(fpoPlayer);

        Event event = Event.builder()
                .year(2025)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .build();
        eventRepository.save(event);

        Tournament mpoTournament = Tournament.builder()
                .division("MPO")
                .place(1)
                .player(mpoPlayer)
                .event(event)
                .build();
        tournamentRepository.save(mpoTournament);

        Tournament fpoTournament = Tournament.builder()
                .division("FPO")
                .place(1)
                .player(fpoPlayer)
                .event(event)
                .build();
        tournamentRepository.save(fpoTournament);

        // Act
        List<Tournament> result = tournamentRepository.findTournamentsByDivision("MPO");

        // Assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.getFirst().getDivision()).isEqualTo("MPO");
        Assertions.assertThat(result.getFirst().getPlayer().getId()).isEqualTo(mpoPlayer.getId());
        Assertions.assertThat(result.getFirst().getEvent().getId()).isEqualTo(event.getId());
    }


}

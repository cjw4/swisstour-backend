package dg.swiss.swiss_dg_db.tournament;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Tournament findFirstByEventId(Long id);

    Tournament findFirstByPlayerId(Long id);

    boolean existsByPlayerIdAndEventId(Long playerId, Long eventId);

    Tournament findByPlayerIdAndEventId(Long playerId, Long eventId);

    @Query(
            "SELECT t FROM Tournament t JOIN FETCH t.event JOIN FETCH t.rounds WHERE t.player.id = :id")
    List<Tournament> findTournamentsWithEventAndRoundsByPlayerId(@Param("id") Long id);

    @Query(
            "SELECT t FROM Tournament t JOIN FETCH t.event JOIN FETCH t.player WHERE t.division = :division")
    List<Tournament> findTournamentsByDivision(@Param("division") String division);
}

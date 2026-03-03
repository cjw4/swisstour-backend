package dg.swiss.swiss_dg_db.tournament;

import dg.swiss.swiss_dg_db.standings.StandingDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Tournament findFirstByEventId(Long id);

    Tournament findFirstByPlayerId(Long id);

    boolean existsByPlayerIdAndEventId(Long playerId, Long eventId);

    Tournament findByPlayerIdAndEventId(Long playerId, Long eventId);

    @Query("SELECT t FROM Tournament t JOIN FETCH t.event JOIN FETCH t.rounds WHERE t.player.id = :id")
    List<Tournament> findTournamentsWithEventAndRoundsByPlayerId(@Param("id") Long id);

    @Query("SELECT new dg.swiss.swiss_dg_db.tournament.TournamentPointsDTO(" +
            "t.division, p.id, e.eventId, p.swisstourLicense, e.isSwisstour, t.points, e.year) " +
            "FROM Tournament t " +
            "JOIN t.player p " +
            "JOIN t.event e " +
            "WHERE t.division = :division")
    List<TournamentPointsDTO> findTournamentPointsByDivision(@Param("division") String division);
}

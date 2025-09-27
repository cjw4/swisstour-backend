package dg.swiss.swiss_dg_db.tournament;

import org.springframework.data.jpa.repository.JpaRepository;


public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Tournament findFirstByEventId(Long id);

    Tournament findFirstByPlayerId(Long id);

    boolean existsByPlayerIdAndEventId(Long playerId, Long eventId);


}

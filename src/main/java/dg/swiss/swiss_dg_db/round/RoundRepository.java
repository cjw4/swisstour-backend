package dg.swiss.swiss_dg_db.round;

import org.springframework.data.jpa.repository.JpaRepository;


public interface RoundRepository extends JpaRepository<Round, Long> {

    Round findFirstByTournamentId(Long id);

    boolean existsByTournamentId(Long id);
}

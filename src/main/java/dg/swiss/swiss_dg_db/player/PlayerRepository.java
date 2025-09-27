package dg.swiss.swiss_dg_db.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByPdgaNumber(Long pdgaNumber);

    boolean existsBySdaNumber(Long sdaNumber);

    boolean existsByFirstnameAndLastname(String firstname, String lastname);

    Optional<Player> findByPdgaNumber(Long pdgaNumber);

    Optional<Player> findByFirstnameAndLastname(String firstname, String lastname);

}

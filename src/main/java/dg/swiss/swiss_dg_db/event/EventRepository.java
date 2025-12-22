package dg.swiss.swiss_dg_db.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsEventById(Long id);

    @Query("SELECT e.id, e.name, e.displayName, e.tier, e.date, e.year, e.numberDays, e.city, e.country, e.numberPlayers, e.points, e.purse, e.isChampionship, e.isSwisstour, e.hasResults," +
            " t.division, t.place, t.rating, t.prize, t.score, t.points," +
            " r.roundNumber, r.rating, r.score " +
            "FROM Event e JOIN e.tournaments t JOIN t.rounds r JOIN t.player p " +
            "WHERE p.id = :id")
    List<Object[]> findRawEventsFromPlayer(@Param("id") Long id);
}

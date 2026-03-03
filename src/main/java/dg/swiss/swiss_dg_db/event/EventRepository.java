package dg.swiss.swiss_dg_db.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByEventId(Long eventId);

    @Query("SELECT e.id, e.eventId, e.name, e.displayName, e.tier, e.year, e.city, e.country, e.numberPlayers, e.points, e.purse, e.isChampionship, e.isSwisstour, e.hasResults," +
            " e.infoLink, e.registrationLink, e.registrationStart, e.swisstourType, e.startDate, e.endDate," +
            " t.division, t.place, t.rating, t.prize, t.score, t.points," +
            " r.roundNumber, r.rating, r.score " +
            "FROM Event e JOIN e.tournaments t JOIN t.rounds r JOIN t.player p " +
            "WHERE p.id = :id")
    List<Object[]> findRawEventsFromPlayer(@Param("id") Long id);
}

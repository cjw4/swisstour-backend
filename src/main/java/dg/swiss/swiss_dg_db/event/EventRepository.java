package dg.swiss.swiss_dg_db.event;

import org.springframework.data.jpa.repository.JpaRepository;


public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsEventById(Long id);

}

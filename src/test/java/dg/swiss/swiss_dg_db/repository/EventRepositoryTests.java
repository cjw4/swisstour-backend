package dg.swiss.swiss_dg_db.repository;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.EventRepository;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class EventRepositoryTests {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void EventRepository_existsByEventId_ReturnsTrue() {
        // Arrange
        Event event = Event.builder()
                .year(2025)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .eventId(80000L)
                .build();
        eventRepository.save(event);

        // Act
        boolean exists = eventRepository.existsByEventId(event.getEventId());

        // Assert
        Assertions.assertThat(event.getEventId()).isEqualTo(80000L);
        Assertions.assertThat(exists).isTrue();
    }

    @Test
    void EventRepository_existsByEventId_ReturnsFalse() {
        // Arrange
        Event event = Event.builder()
                .year(2025)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .eventId(80000L)
                .build();
        eventRepository.save(event);

        // Act
        boolean exists = eventRepository.existsByEventId(70000L);

        // Assert
        Assertions.assertThat(exists).isFalse();
    }


}

package dg.swiss.swiss_dg_db.service;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.EventDTO;
import dg.swiss.swiss_dg_db.event.EventRepository;
import dg.swiss.swiss_dg_db.event.EventService;
import dg.swiss.swiss_dg_db.player.Player;
import dg.swiss.swiss_dg_db.tournament.Tournament;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
public class EventServiceTests {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event event2024;
    private Event event2025;
    private Event event2025MPO;
    private Tournament tournamentMPO;
    private Tournament tournamentFPO;
    private Player paulMcBeth;
    private Player silvaSaarinen;

    @BeforeEach
    public void setUp() {
        paulMcBeth = Player.builder()
                .firstname("Paul")
                .lastname("McBeth")
                .swisstourLicense(false)
                .isPro(true)
                .build();
        silvaSaarinen = Player.builder()
                .firstname("Silva")
                .lastname("Saarinen")
                .swisstourLicense(false)
                .isPro(true)
                .build();
        tournamentMPO = Tournament.builder()
                .division("MPO")
                .place(1)
                .player(paulMcBeth)
                .build();
        tournamentFPO = Tournament.builder()
                .division("FPO")
                .place(1)
                .player(silvaSaarinen)
                .build();
        event2024 = Event.builder()
                .id(1L)
                .year(2024)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .startDate(LocalDate.of(2024,10,31))
                .tournaments(Set.of(tournamentMPO))
                .build();
        event2025 = Event.builder()
                .id(1L)
                .year(2025)
                .points(200)
                .isChampionship(false)
                .isSwisstour(true)
                .startDate(LocalDate.of(2025,7,4))
                .tournaments(Set.of(tournamentFPO))
                .build();
        event2025MPO = Event.builder()
                .id(1L)
                .year(2025)
                .points(200)
                .isChampionship(false)
                .isSwisstour(true)
                .startDate(LocalDate.of(2025,7,4))
                .tournaments(Set.of(tournamentMPO))
                .build();

    }

    @Nested
    @DisplayName("findAll() method tests")
    class FindAllTests {
        @Test
        @DisplayName("no inputs")
        void EventService_findAllNoInputs_ReturnsListOfEventDTO() {
            // Arrange
            when(eventRepository.findAll(Sort.by("startDate")))
                    .thenReturn(List.of(event2024, event2025));

            // Act
            List<EventDTO> result = eventService.findAll(null, null);

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("year as input")
        void EventService_findAllWithYear_ReturnsListOfEventDTO() {
            // Arrange
            when(eventRepository.findAll(Sort.by("startDate")))
                    .thenReturn(List.of(event2024, event2025));

            // Act
            List<EventDTO> result = eventService.findAll(2024, null);

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result.getFirst().getYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("division as input")
        void EventService_findAllWithDivision_ReturnsListOfEventDTO() {
            // Arrange
            when(eventRepository.findAll(Sort.by("startDate")))
                    .thenReturn(List.of(event2024, event2025));

            // Act
            List<EventDTO> result = eventService.findAll(null, "MPO");

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("year and division as input")
        void EventService_findAllWithYearAndDivision_ReturnsListOfEventDTO() {
            // Arrange
            when(eventRepository.findAll(Sort.by("startDate")))
                    .thenReturn(List.of(event2024, event2025, event2025MPO));

            // Act
            List<EventDTO> result = eventService.findAll(2024, "MPO");

            // Assert
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(1);
        }
    }



    @Test
    void EventService_create_ReturnsEventDTO() {
        // Arrange
        Event event = Event.builder()
                .id(1L)
                .year(2024)
                .points(200)
                .displayName("ZDGO")
                .isChampionship(false)
                .isSwisstour(true)
                .eventId(80000L)
                .hasResults(false)
                .build();
        EventDTO eventDTO = EventDTO.builder()
                .year(2024)
                .points(200)
                .displayName("ZDGO")
                .isChampionship(false)
                .isSwisstour(true)
                .eventId(80000L)
                .build();

        // Act
        when(eventRepository.save(Mockito.any(Event.class))).thenReturn(event);
        EventDTO savedEventDTO = eventService.create(eventDTO);

        // Assert
        Assertions.assertThat(savedEventDTO).isNotNull();
        Assertions.assertThat(savedEventDTO.getId()).isEqualTo(event.getId());
        Assertions.assertThat(savedEventDTO.getHasResults()).isEqualTo(false);
    }

}

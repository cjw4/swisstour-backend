package dg.swiss.swiss_dg_db.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = EventResource.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EventResource Unit Tests")
class EventResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        eventDTO = EventDTO.builder()
                .eventId(1L)
                .points(100)
                .isChampionship(false)
                .isSwisstour(true)
                .build();
    }

    @Nested
    @DisplayName("getEvents() method tests")
    class GetEventsTest {
        @Test
        @DisplayName("test successful response")
        void EventResource_getEventsSuccess_Returns200() throws Exception {
            // Arrange
            when(eventService.findAll(any(Integer.class), isNull()))
                    .thenReturn(List.of(eventDTO));

            // Act
            ResultActions response = mockMvc.perform(MockMvcRequestBuilders.get("/api/events")
                    .param("year", "2025"));

            // Assert
            response.andExpect(MockMvcResultMatchers.status().isOk());
            verify(eventService, times(1)).findAll(any(Integer.class), isNull());
        }
    }

    @Nested
    @DisplayName("createEvent() method tests")
    class CreateEventTest {
        @Test
        @DisplayName("test successful creation")
        void EventResource_createSuccess_ReturnEventDTO() throws Exception {
            // Arrange
            when(eventRepository.existsByEventId(eventDTO.getEventId()))
                    .thenReturn(false);
            when(eventService.addDetails(eventDTO))
                    .thenReturn(eventDTO);
            when(eventService.create(eventDTO))
                    .thenReturn(eventDTO);

            // Act
            ResultActions response = mockMvc.perform(post("/api/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(eventDTO)));

            // Assert
            response.andExpect(MockMvcResultMatchers.status().isCreated());
            verify(eventRepository, times(1)).existsByEventId(eventDTO.getEventId());
            verify(eventService, times(1)).addDetails(eventDTO);
            verify(eventService, times(1)).create(eventDTO);
        }

        @Test
        @DisplayName("test creation with duplicate eventId")
        void EventResource_createDuplicateEventId_ReturnConflict() throws Exception {
            // Arrange
            when(eventRepository.existsByEventId(eventDTO.getEventId()))
                    .thenReturn(true);

            // Act
            ResultActions response = mockMvc.perform(post("/api/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(eventDTO)));

            // Assert
            response.andExpect(MockMvcResultMatchers.status().isConflict());
            verify(eventRepository, times(1)).existsByEventId(eventDTO.getEventId());
            verifyNoInteractions(eventService);
        }
    }
}
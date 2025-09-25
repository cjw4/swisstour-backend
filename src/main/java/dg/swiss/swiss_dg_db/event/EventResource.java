package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.scrape.EventDetails;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventResource {

    private final EventService eventService;

    public EventResource(final EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(eventService.get(id));
    }

    @PostMapping
    public ResponseEntity<Long> createEvent(@RequestBody @Valid final EventDTO eventDTO) throws IOException {
        EventDTO eventDTOwDetails = eventService.addDetails(eventDTO);
        final Long createdId = eventService.create(eventDTOwDetails);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PostMapping("/results/{id}")
    public ResponseEntity<Long> getEventResults(@PathVariable(name = "id") final Long id) throws IOException {
        eventService.addTournaments(id);
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateEvent(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final EventDTO eventDTO) throws IOException {
        EventDTO eventDTOwDetails = eventService.addDetails(eventDTO);
        if (!eventDTOwDetails.getId().equals(id)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        eventService.update(id, eventDTOwDetails);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable(name = "id") final Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

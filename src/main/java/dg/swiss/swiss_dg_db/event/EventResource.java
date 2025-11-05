package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.exceptions.EventAlreadyExistsException;
import dg.swiss.swiss_dg_db.exceptions.TooManyRequestsException;
import dg.swiss.swiss_dg_db.player.PlayerDTO;
import dg.swiss.swiss_dg_db.player.PlayerResource;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.scrape.EventDetails;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("http://localhost:4200")
@RestController
@RequestMapping(value = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventResource {

    private final EventService eventService;
    private final EventRepository eventRepository;

    public EventResource(final EventService eventService,
                         final PlayerService playerService,
                         final EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
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
    public ResponseEntity<EventDTO> createEvent(@RequestBody @Valid final EventDTO eventDTO) throws IOException {
        if (!eventRepository.existsEventById(eventDTO.getId())) {
            EventDTO eventDTOwDetails = eventService.addDetails(eventDTO);
            eventService.create(eventDTOwDetails);
            return new ResponseEntity<>(eventDTOwDetails, HttpStatus.CREATED);
        }
        throw new EventAlreadyExistsException();
    }

    @PostMapping("/results/{id}")
    public ResponseEntity<Long> getEventResults(@PathVariable(name = "id") final Long id) throws IOException {
        if (!eventRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        EventDetails eventDetails = eventService.addTournaments(id);

        eventDetails.getTournaments().forEach(tournamentDetail -> {
            try {
                eventService.addPlayerFromEvent(tournamentDetail);
            } catch (IOException | InterruptedException e) {
                throw new TooManyRequestsException();
            }
            eventService.addTournamentFromEvent(id, tournamentDetail);
        });
        eventService.toggleHasResults(id);
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

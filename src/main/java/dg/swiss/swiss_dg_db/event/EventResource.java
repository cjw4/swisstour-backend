package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.exceptions.EventAlreadyExistsException;
import dg.swiss.swiss_dg_db.exceptions.TooManyRequestsException;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.scrape.EventDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventResource {

    private final EventService eventService;
    private final EventRepository eventRepository;

    public EventResource(final EventService eventService,
                         final PlayerService playerService,
                         final EventRepository eventRepository, EventDetails eventDetails) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<List<EventDTO>> getEventsByYear(@PathVariable Integer year, @RequestParam(required = false) String division) {
        if (division != null) {
            return ResponseEntity.ok(eventService.findByYearAndDivision(year, division));
        } else {
            return ResponseEntity.ok(eventService.findByYear(year));
        }
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
        // make sure that the eventId is found in the database
        if (!eventRepository.existsById(id)) { return new ResponseEntity<>(HttpStatus.NOT_FOUND); }

        // re-collect the event details and scape tournament results (also where swisstour points are added)
        EventDetails eventDetails = eventService.addTournaments(id);

        // for each of the tournaments in the event
        for (EventDetails.TournamentDetail tournamentDetail : eventDetails.getTournaments()) {
            try {
                eventService.addPlayerFromEvent(tournamentDetail);
                eventService.addTournamentFromEvent(id, tournamentDetail);
            } catch (InterruptedException e) {
                throw new TooManyRequestsException();
            } catch (IOException e) {
                // System.err.println("Skipping tournament due to error fetching player details: " + e.getMessage());
                continue;
            }
        }

        eventService.toggleHasResults(id);
        return new ResponseEntity<>(id, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable(name = "id") final Long id,
            @RequestBody @Valid final EventDTO eventDTO) throws IOException {
        // get the pts before and after change
        Integer ptsBefore = eventRepository.findById(id).get().getPoints();
        Integer ptsAfter = eventDTO.getPoints();

        // add the details to the eventDTO from what was previously in the database
        EventDTO eventDTOwDetails = eventService.addDetails(eventDTO);
        eventService.update(id, eventDTOwDetails);

        // make sure that the ids from event DTO from the request body is equal to that in the path
        if (!eventDTOwDetails.getId().equals(id)) { return new ResponseEntity<>(HttpStatus.CONFLICT); }

        // remove all tournaments assigned to the event -> necessary to do if the points were changed
        if (!Objects.equals(ptsBefore, ptsAfter)) {
            eventService.deleteTournaments(id);
            if (eventDTOwDetails.getHasResults()) {
                eventService.toggleHasResults(id);
            }
        }

        return ResponseEntity.ok(eventDTOwDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable(name = "id") final Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

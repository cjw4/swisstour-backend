package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.exceptions.EventAlreadyExistsException;
import dg.swiss.swiss_dg_db.exceptions.TooManyRequestsException;
import dg.swiss.swiss_dg_db.scrape.EventDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/events", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Events")
@AllArgsConstructor
public class EventResource {

    private final EventService eventService;
    private final EventRepository eventRepository;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<EventDTO>> getEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String division)
    {
        return ResponseEntity.ok(eventService.getEvents(year, division));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEvent(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventDTO> createEvent(@RequestBody @Valid final EventDTO eventDTO) throws IOException {
        if (eventDTO.getEventId() != null && eventRepository.existsByEventId(eventDTO.getEventId())) {
            throw new EventAlreadyExistsException();
        }
        EventDTO eventDTOwDetails = eventService.addDetails(eventDTO);
        EventDTO createdEventDTO = eventService.create(eventDTOwDetails);
        return new ResponseEntity<>(createdEventDTO, HttpStatus.CREATED);

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
        eventDTOwDetails.setId(id);
        eventService.update(id, eventDTOwDetails);

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

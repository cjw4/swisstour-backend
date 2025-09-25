package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.events.BeforeDeleteEvent;
import dg.swiss.swiss_dg_db.player.PlayerDTO;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.scrape.EventDetails;
import dg.swiss.swiss_dg_db.util.NotFoundException;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher publisher;
    private final EventDetails eventDetails;
    private final PlayerService playerService;

    public EventService(final EventRepository eventRepository,
                        final ApplicationEventPublisher publisher,
                        final EventDetails eventDetails,
                        final PlayerService playerService) {
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.eventDetails = eventDetails;
        this.playerService = playerService;
    }

    public List<EventDTO> findAll() {
        final List<Event> events = eventRepository.findAll(Sort.by("id"));
        return events.stream()
                .map(event -> mapToDTO(event, new EventDTO()))
                .toList();
    }

    public EventDTO get(final Long id) {
        return eventRepository.findById(id)
                .map(event -> mapToDTO(event, new EventDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public EventDTO addDetails(EventDTO eventDTO) throws IOException {
        eventDetails.scrapeEventInfo(eventDTO.getId());
        eventDTO.setName(eventDetails.getName());
        eventDTO.setDate(eventDetails.getDate());
        eventDTO.setNumberDays(eventDetails.getNumberDays());
        eventDTO.setTier(eventDetails.getTier());
        eventDTO.setNumberPlayers(eventDetails.getNumberPlayers());
        eventDTO.setPurse(eventDetails.getPurse());
        eventDTO.setCity(eventDetails.getCity());
        eventDTO.setCountry(eventDetails.getCountry());
        return eventDTO;
    }

    public void addTournaments(final Long id) throws IOException {
        eventDetails.scrapeEventInfo(id);
        eventDetails.scrapeEventResults(id);
        // This is where I would add the tournaments into the database
        eventDetails.getTournaments().forEach(tournamentDetail -> {
                 Long pdgaNumber = tournamentDetail.getPdgaNumber();
                 if (pdgaNumber != null && !playerService.pdgaNumberExists(pdgaNumber)) {
                     PlayerDTO playerDTO = new PlayerDTO();
                     playerDTO.setPdgaNumber(pdgaNumber);
                     try {
                         Thread.sleep(2000);
                         playerService.addDetails(playerDTO);
                     } catch (IOException | InterruptedException e) {
                         throw new RuntimeException(e);
                     }
                     playerDTO.setSwisstourLicense(false);
                     System.out.println("Adding Player: " + playerDTO.getFirstname() + " " + playerDTO.getLastname());
                     playerService.create(playerDTO);
                 }
                });
    }

    public Long create(final EventDTO eventDTO) {
        final Event event = new Event();
        mapToEntity(eventDTO, event);
        return eventRepository.save(event).getId();
    }

    public void update(final Long id, final EventDTO eventDTO) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(eventDTO, event);
        eventRepository.save(event);
    }

    public void delete(final Long id) {
        final Event event = eventRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteEvent(id));
        eventRepository.delete(event);
    }

    private EventDTO mapToDTO(final Event event, final EventDTO eventDTO) {
        eventDTO.setId(event.getId());
        eventDTO.setName(event.getName());
        eventDTO.setDisplayName(event.getDisplayName());
        eventDTO.setTier(event.getTier());
        eventDTO.setDate(event.getDate());
        eventDTO.setNumberDays(event.getNumberDays());
        eventDTO.setCity(event.getCity());
        eventDTO.setCountry(event.getCountry());
        eventDTO.setNumberPlayers(event.getNumberPlayers());
        eventDTO.setPoints(event.getPoints());
        eventDTO.setPurse(event.getPurse());
        eventDTO.setIsChampionship(event.getIsChampionship());
        eventDTO.setIsSwisstour(event.getIsSwisstour());
        return eventDTO;
    }

    private Event mapToEntity(final EventDTO eventDTO, final Event event) {
        event.setId(eventDTO.getId());
        event.setName(eventDTO.getName());
        event.setDisplayName(eventDTO.getDisplayName());
        event.setTier(eventDTO.getTier());
        event.setDate(eventDTO.getDate());
        event.setNumberDays(eventDTO.getNumberDays());
        event.setCity(eventDTO.getCity());
        event.setCountry(eventDTO.getCountry());
        event.setNumberPlayers(eventDTO.getNumberPlayers());
        event.setPoints(eventDTO.getPoints());
        event.setPurse(eventDTO.getPurse());
        event.setIsChampionship(eventDTO.getIsChampionship());
        event.setIsSwisstour(eventDTO.getIsSwisstour());
        return event;
    }

}

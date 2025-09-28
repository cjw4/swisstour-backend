package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.events.BeforeDeleteEvent;
import dg.swiss.swiss_dg_db.player.PlayerDTO;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.round.RoundDTO;
import dg.swiss.swiss_dg_db.round.RoundRepository;
import dg.swiss.swiss_dg_db.round.RoundResource;
import dg.swiss.swiss_dg_db.scrape.EventDetails;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import dg.swiss.swiss_dg_db.tournament.TournamentDTO;
import dg.swiss.swiss_dg_db.tournament.TournamentService;
import dg.swiss.swiss_dg_db.util.NotFoundException;

import java.io.IOException;
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
    private final TournamentService tournamentService;
    private final RoundResource roundResource;
    private final RoundRepository roundRepository;

    public EventService(final EventRepository eventRepository,
                        final ApplicationEventPublisher publisher,
                        final EventDetails eventDetails,
                        final PlayerService playerService,
                        final TournamentService tournamentService,
                        final RoundResource roundResource, RoundRepository roundRepository) {
        this.eventRepository = eventRepository;
        this.publisher = publisher;
        this.eventDetails = eventDetails;
        this.playerService = playerService;
        this.tournamentService = tournamentService;
        this.roundResource = roundResource;
        this.roundRepository = roundRepository;
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

    public EventDetails addTournaments(final Long id) throws IOException {
        eventDetails.scrapeEventInfo(id);
        eventDetails.scrapeEventResults(id);
        return eventDetails;
    }

    public void addPlayerFromEvent(EventDetails.TournamentDetail tournamentDetail) {
        // Add player to database if not yet there
        Long pdgaNumber = tournamentDetail.getPdgaNumber();
        String name = tournamentDetail.getName();
        PlayerDTO playerDTO = new PlayerDTO();
        // Search by PDGA Number
        if (pdgaNumber != null && !playerService.pdgaNumberExists(pdgaNumber)) {
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
            // Search by Name
        } else if (pdgaNumber == null && !playerService.nameExists(name)) {
            String[] names = name.trim().split("\\s+");
            NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
            playerDTO.setFirstname(nameInfo.getFirstName());
            playerDTO.setLastname(nameInfo.getLastName());
            playerDTO.setIsPro(false);
            playerDTO.setSwisstourLicense(false);
            System.out.println("Adding Player: " + playerDTO.getFirstname() + " " + playerDTO.getLastname());
            playerService.create(playerDTO);
        }
    }

    public void addTournamentFromEvent(Long eventId,
                                       EventDetails.TournamentDetail tournamentDetail) {
        Long pdgaNumber = tournamentDetail.getPdgaNumber();
        String name = tournamentDetail.getName();
        Long playerId = null;
        if (pdgaNumber != null && playerService.pdgaNumberExists(pdgaNumber)) {
            PlayerDTO playerDTO = playerService.findByPdgaNumber(pdgaNumber);
            playerId = playerDTO.getId();
        } else if (pdgaNumber == null && playerService.nameExists(name)) {
            PlayerDTO playerDTO = playerService.findByName(name);
            playerId = playerDTO.getId();
        }
        // Create TournamentDTO
        TournamentDTO tournamentDTO = new TournamentDTO();
        tournamentDTO.setEvent(eventId);
        tournamentDTO.setPlayer(playerId);
        tournamentDTO.setDivision(tournamentDetail.getDivision());
        tournamentDTO.setPlace(tournamentDetail.getPlace());
        tournamentDTO.setPrize(tournamentDetail.getPrize());
        tournamentDTO.setScore(tournamentDetail.getScore());
        Long tournamentId = tournamentService.create(tournamentDTO);
        if (tournamentId != null && !roundRepository.existsByTournamentId(tournamentId)) {
            // Create RoundDTOs
            List<EventDetails.RoundDetail> rounds = tournamentDetail.getRounds();
            for (EventDetails.RoundDetail roundDetail : rounds) {
                RoundDTO roundDTO = new RoundDTO();
                roundDTO.setRoundNumber(roundDetail.getRoundNumber());
                roundDTO.setScore(roundDetail.getScore());
                roundDTO.setRating(roundDetail.getRating());
                roundDTO.setTournament(tournamentId);
                roundResource.createRound(roundDTO);
            }
        }
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

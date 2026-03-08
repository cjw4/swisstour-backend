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

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher publisher;
    private final EventDetails eventDetails;
    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final RoundResource roundResource;
    private final RoundRepository roundRepository;

    @Transactional
    public List<EventDTO> getEvents(Integer year, String division) {
        List<Event> events = eventRepository.findAll(Sort.by("startDate"));
        if (year == null && division == null) {
            return events.stream().map(e -> mapToDTO(e, new EventDTO())).toList();
        }
        if (division == null) {
            return events.stream()
                    .filter(e -> e.getYear().equals(year))
                    .map(e -> mapToDTO(e, new EventDTO()))
                    .toList();
        }
        if (year == null) {
            return events.stream()
                    .filter(
                            e -> {
                                var tournaments = e.getTournaments();
                                return tournaments.stream()
                                        .anyMatch(t -> t.getDivision().equals(division));
                            })
                    .map(e -> mapToDTO(e, new EventDTO()))
                    .toList();
        } else {
            return events.stream()
                    .filter(e -> e.getYear().equals(year))
                    .filter(
                            e -> {
                                var tournaments = e.getTournaments();
                                return tournaments.stream()
                                        .anyMatch(t -> t.getDivision().equals(division));
                            })
                    .map(e -> mapToDTO(e, new EventDTO()))
                    .toList();
        }
    }

    public EventDTO getEvent(final Long id) {
        return eventRepository
                .findById(id)
                .map(event -> mapToDTO(event, new EventDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public EventDTO addDetails(EventDTO eventDTO) throws IOException {
        if (eventDTO.getEventId() != null) {
            eventDetails.scrapeEventInfo(eventDTO.getEventId());
            eventDTO.setName(eventDetails.getName());
            eventDTO.setStartDate(eventDetails.getDate());
            if (eventDetails.getNumberDays() > 0) {
                eventDTO.setEndDate(
                        eventDetails.getDate().plusDays(eventDetails.getNumberDays() - 1));
            } else {
                eventDTO.setEndDate(eventDetails.getDate());
            }
            eventDTO.setTier(eventDetails.getTier());
            eventDTO.setNumberPlayers(eventDetails.getNumberPlayers());
            eventDTO.setPurse(eventDetails.getPurse());
            eventDTO.setCity(eventDetails.getCity());
            eventDTO.setCountry(eventDetails.getCountry());
            eventDTO.setHasResults(eventDetails.isHasResults());
        } else {
            eventDTO.setHasResults(false);
        }
        return eventDTO;
    }

    public EventDetails addTournaments(final Long id) throws IOException {
        // get the event by PK to retrieve the eventId for scraping
        Event event = eventRepository.findById(id).orElseThrow(NotFoundException::new);
        Long eventId = event.getEventId();
        // go and get the event info again (in case anything has been updated on the pdga website)
        eventDetails.scrapeEventInfo(eventId);
        // scrape the tournaments from the event results on pdga website
        eventDetails.scrapeEventResults(eventId, event.getPoints());
        return eventDetails;
    }

    public void addPlayerFromEvent(EventDetails.TournamentDetail tournamentDetail)
            throws IOException, InterruptedException {
        // Add player to database if not yet there
        Long pdgaNumber = tournamentDetail.getPdgaNumber();
        String name = tournamentDetail.getName();
        PlayerDTO playerDTO;
        // Search by PDGA Number
        if (pdgaNumber != null && !playerService.pdgaNumberExists(pdgaNumber)) {
            // Catch the case of a player exists in the database, and they now have a pdga number
            if (playerService.nameExists(name)) {
                playerDTO = playerService.findByName(name);
            } else {
                playerDTO = new PlayerDTO();
            }

            playerDTO.setPdgaNumber(pdgaNumber);
            playerService.addDetails(playerDTO);
            Thread.sleep(2000);
            if (playerService.nameExists(name)) {
                System.out.println(
                        "Updating player, now PDGA registered, in database: "
                                + playerDTO.getFirstname()
                                + " "
                                + playerDTO.getLastname());
                playerService.update(playerDTO.getId(), playerDTO);
            } else {
                playerDTO.setSwisstourLicense(false);
                System.out.println(
                        "Adding registered PDGA player to database: "
                                + playerDTO.getFirstname()
                                + " "
                                + playerDTO.getLastname());
                playerService.create(playerDTO);
            }

            // Search by Name
        } else if (pdgaNumber == null && !playerService.nameExists(name)) {
            playerDTO = new PlayerDTO();
            String[] names = name.trim().split("\\s+");
            NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
            playerDTO.setFirstname(nameInfo.getFirstName());
            playerDTO.setLastname(nameInfo.getLastName());
            playerDTO.setIsPro(false);
            playerDTO.setSwisstourLicense(false);
            System.out.println(
                    "Adding non-registered PDGA player to database: "
                            + playerDTO.getFirstname()
                            + " "
                            + playerDTO.getLastname());
            playerService.create(playerDTO);
        }
    }

    public void addTournamentFromEvent(Long id, EventDetails.TournamentDetail tournamentDetail) {
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
        tournamentDTO.setEvent(id);
        tournamentDTO.setPlayer(playerId);
        tournamentDTO.setDivision(tournamentDetail.getDivision());
        tournamentDTO.setPlace(tournamentDetail.getPlace());
        tournamentDTO.setPrize(tournamentDetail.getPrize());
        tournamentDTO.setPoints(tournamentDetail.getPoints());
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

    public void toggleHasResults(final Long id) {
        eventRepository
                .findById(id)
                .ifPresent(
                        e -> {
                            e.setHasResults(!e.getHasResults());
                            eventRepository.save(e);
                        });
    }

    public EventDTO create(final EventDTO eventDTO) {
        final Event event = new Event();
        eventDTO.setHasResults(false);
        mapToEntity(eventDTO, event);
        Event savedEvent = eventRepository.save(event);
        eventDTO.setId(savedEvent.getId());
        return eventDTO;
    }

    public void update(final Long id, final EventDTO eventDTO) {
        final Event event = eventRepository.findById(id).orElseThrow(NotFoundException::new);
        mapToEntity(eventDTO, event);
        eventRepository.save(event);
    }

    @Transactional
    public void delete(final Long id) {
        final Event event = eventRepository.findById(id).orElseThrow(NotFoundException::new);
        eventRepository.delete(event);
        publisher.publishEvent(new BeforeDeleteEvent(id));
    }

    @Transactional
    public void deleteTournaments(final Long id) {
        Event event =
                eventRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Event not found:" + id));
        event.getTournaments().clear();
        eventRepository.save(event);
    }

    private EventDTO mapToDTO(final Event event, final EventDTO eventDTO) {
        eventDTO.setId(event.getId());
        eventDTO.setEventId(event.getEventId());
        eventDTO.setName(event.getName());
        eventDTO.setDisplayName(event.getDisplayName());
        eventDTO.setTier(event.getTier());
        eventDTO.setYear(event.getYear());
        eventDTO.setCity(event.getCity());
        eventDTO.setCountry(event.getCountry());
        eventDTO.setNumberPlayers(event.getNumberPlayers());
        eventDTO.setPoints(event.getPoints());
        eventDTO.setPurse(event.getPurse());
        eventDTO.setIsChampionship(event.getIsChampionship());
        eventDTO.setIsSwisstour(event.getIsSwisstour());
        eventDTO.setHasResults(event.getHasResults());
        eventDTO.setInfoLink(event.getInfoLink());
        eventDTO.setRegistrationLink(event.getRegistrationLink());
        eventDTO.setRegistrationStart(event.getRegistrationStart());
        eventDTO.setSwisstourType(event.getSwisstourType());
        eventDTO.setStartDate(event.getStartDate());
        eventDTO.setEndDate(event.getEndDate());
        return eventDTO;
    }

    private Event mapToEntity(final EventDTO eventDTO, final Event event) {
        event.setEventId(eventDTO.getEventId());
        event.setName(eventDTO.getName());
        event.setDisplayName(eventDTO.getDisplayName());
        event.setTier(eventDTO.getTier());
        event.setYear(eventDTO.getYear());
        event.setCity(eventDTO.getCity());
        event.setCountry(eventDTO.getCountry());
        event.setNumberPlayers(eventDTO.getNumberPlayers());
        event.setPoints(eventDTO.getPoints());
        event.setPurse(eventDTO.getPurse());
        event.setIsChampionship(eventDTO.getIsChampionship());
        event.setIsSwisstour(eventDTO.getIsSwisstour());
        event.setHasResults(eventDTO.getHasResults());
        event.setInfoLink(eventDTO.getInfoLink());
        event.setRegistrationLink(eventDTO.getRegistrationLink());
        event.setRegistrationStart(eventDTO.getRegistrationStart());
        event.setSwisstourType(eventDTO.getSwisstourType());
        event.setStartDate(eventDTO.getStartDate());
        event.setEndDate(eventDTO.getEndDate());
        return event;
    }
}

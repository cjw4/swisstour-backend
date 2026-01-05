package dg.swiss.swiss_dg_db.tournament;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.EventRepository;
import dg.swiss.swiss_dg_db.events.BeforeDeleteEvent;
import dg.swiss.swiss_dg_db.events.BeforeDeletePlayer;
import dg.swiss.swiss_dg_db.events.BeforeDeleteTournament;
import dg.swiss.swiss_dg_db.player.Player;
import dg.swiss.swiss_dg_db.player.PlayerRepository;
import dg.swiss.swiss_dg_db.util.NotFoundException;
import dg.swiss.swiss_dg_db.util.ReferencedException;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final EventRepository eventRepository;
    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher publisher;

    public TournamentService(final TournamentRepository tournamentRepository,
            final EventRepository eventRepository, final PlayerRepository playerRepository,
            final ApplicationEventPublisher publisher) {
        this.tournamentRepository = tournamentRepository;
        this.eventRepository = eventRepository;
        this.playerRepository = playerRepository;
        this.publisher = publisher;
    }

    public List<TournamentDTO> findAll() {
        final List<Tournament> tournaments = tournamentRepository.findAll(Sort.by("id"));
        return tournaments.stream()
                .map(tournament -> mapToDTO(tournament, new TournamentDTO()))
                .toList();
    }

    public TournamentDTO get(final Long id) {
        return tournamentRepository.findById(id)
                .map(tournament -> mapToDTO(tournament, new TournamentDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final TournamentDTO tournamentDTO) {
        final Tournament tournament = new Tournament();
        mapToEntity(tournamentDTO, tournament);
        // save the tournament if it one does not already exist for that player
        if (!tournamentRepository.existsByPlayerIdAndEventId(tournamentDTO.getPlayer(), tournamentDTO.getEvent())) {
            this.playerRepository.findById(tournamentDTO.getPlayer())
                    .ifPresentOrElse(player -> {
                        System.out.println("Adding tournament to database for: " + player.getFirstname() + " " + player.getLastname());
                    }, () -> {
                        System.out.println("Player was not found in the database, no tournament created.");
                    });
            return tournamentRepository.save(tournament).getId();
        } else {
            return tournamentRepository.findByPlayerIdAndEventId(tournamentDTO.getPlayer(), tournamentDTO.getEvent()).getId();
        }
    }

    public void update(final Long id, final TournamentDTO tournamentDTO) {
        final Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(tournamentDTO, tournament);
        tournamentRepository.save(tournament);
    }

    public void delete(final Long id) {
        final Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteTournament(id));
        tournamentRepository.delete(tournament);
    }

    private TournamentDTO mapToDTO(final Tournament tournament, final TournamentDTO tournamentDTO) {
        tournamentDTO.setId(tournament.getId());
        tournamentDTO.setDivision(tournament.getDivision());
        tournamentDTO.setPlace(tournament.getPlace());
        tournamentDTO.setRating(tournament.getRating());
        tournamentDTO.setPrize(tournament.getPrize());
        tournamentDTO.setScore(tournament.getScore());
        tournamentDTO.setPoints(tournament.getPoints());
        tournamentDTO.setEvent(tournament.getEvent() == null ? null : tournament.getEvent().getId());
        tournamentDTO.setPlayer(tournament.getPlayer() == null ? null : tournament.getPlayer().getId());
        return tournamentDTO;
    }

    private Tournament mapToEntity(final TournamentDTO tournamentDTO, final Tournament tournament) {
        tournament.setDivision(tournamentDTO.getDivision());
        tournament.setPlace(tournamentDTO.getPlace());
        tournament.setRating(tournamentDTO.getRating());
        tournament.setPrize(tournamentDTO.getPrize());
        tournament.setScore(tournamentDTO.getScore());
        tournament.setPoints(tournamentDTO.getPoints());
        final Event event = tournamentDTO.getEvent() == null ? null : eventRepository.findById(tournamentDTO.getEvent())
                .orElseThrow(() -> new NotFoundException("event not found"));
        tournament.setEvent(event);
        final Player player = tournamentDTO.getPlayer() == null ? null : playerRepository.findById(tournamentDTO.getPlayer())
                .orElseThrow(() -> new NotFoundException("player not found"));
        tournament.setPlayer(player);
        return tournament;
    }

    @EventListener(BeforeDeleteEvent.class)
    public void on(final BeforeDeleteEvent event) {
        final ReferencedException referencedException = new ReferencedException();
        final Tournament eventTournament = tournamentRepository.findFirstByEventId(event.getId());
        if (eventTournament != null) {
            referencedException.setKey("event.tournament.event.referenced");
            referencedException.addParam(eventTournament.getId());
            throw referencedException;
        }
    }

    @EventListener(BeforeDeletePlayer.class)
    public void on(final BeforeDeletePlayer event) {
        final ReferencedException referencedException = new ReferencedException();
        final Tournament playerTournament = tournamentRepository.findFirstByPlayerId(event.getId());
        if (playerTournament != null) {
            referencedException.setKey("player.tournament.player.referenced");
            referencedException.addParam(playerTournament.getId());
            throw referencedException;
        }
    }

}

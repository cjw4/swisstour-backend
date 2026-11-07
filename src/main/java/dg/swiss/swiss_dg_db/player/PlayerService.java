package dg.swiss.swiss_dg_db.player;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.PlayerEventsDTO;
import dg.swiss.swiss_dg_db.events.BeforeDeletePlayer;
import dg.swiss.swiss_dg_db.pdga_api.PdgaApiPlayerResponseDTO;
import dg.swiss.swiss_dg_db.pdga_api.PdgaApiPlayerService;
import dg.swiss.swiss_dg_db.round.RoundDTOsmall;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import dg.swiss.swiss_dg_db.tournament.Tournament;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import dg.swiss.swiss_dg_db.util.NotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher publisher;
    private final PdgaApiPlayerService pdgaApiPlayerService;
    private final TournamentRepository tournamentRepository;

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    public List<PlayerDTO> findAll() {
        final List<Player> players = playerRepository.findAll(Sort.by("id"));
        return players.stream().map(player -> mapToDTO(player, new PlayerDTO())).toList();
    }

    public PlayerDTO get(final Long id) {
        PlayerDTO playerDTO =
                playerRepository
                        .findById(id)
                        .map(player -> mapToDTO(player, new PlayerDTO()))
                        .orElseThrow(NotFoundException::new);
        logger.info(
                "Player information for {} {} was requested.",
                playerDTO.getFirstname(),
                playerDTO.getLastname());
        return playerDTO;
    }

    public PlayerDTO addDetails(PlayerDTO playerDTO) throws IOException {
        if (playerDTO.getPdgaNumber() != null) {
            PdgaApiPlayerResponseDTO pdgaApiPlayerDTO =
                    pdgaApiPlayerService.getPlayerInfo(playerDTO.getPdgaNumber());
            playerDTO.setFirstname(pdgaApiPlayerDTO.getFirst_name());
            playerDTO.setLastname(pdgaApiPlayerDTO.getLast_name());
            playerDTO.setIsPro("P".equals(pdgaApiPlayerDTO.getPlayerClass()));
        } else {
            playerDTO.setIsPro(false);
        }
        return playerDTO;
    }

    public Long create(final PlayerDTO playerDTO) {
        final Player player = new Player();
        mapToEntity(playerDTO, player);
        return playerRepository.save(player).getId();
    }

    public void update(final Long id, final PlayerDTO playerDTO) {
        final Player player = playerRepository.findById(id).orElseThrow(NotFoundException::new);
        mapToEntity(playerDTO, player);
        playerRepository.save(player);
    }

    @Transactional
    public void delete(final Long id) {
        final Player player = playerRepository.findById(id).orElseThrow(NotFoundException::new);

        // Explicitly clear relationships
        player.getTournaments().clear();
        playerRepository.save(player);

        publisher.publishEvent(new BeforeDeletePlayer(id));
        playerRepository.delete(player);
    }

    private PlayerDTO mapToDTO(final Player player, final PlayerDTO playerDTO) {
        playerDTO.setId(player.getId());
        playerDTO.setFirstname(player.getFirstname());
        playerDTO.setLastname(player.getLastname());
        playerDTO.setPdgaNumber(player.getPdgaNumber());
        playerDTO.setSdaNumber(player.getSdaNumber());
        playerDTO.setSwisstourLicense(player.getSwisstourLicense());
        playerDTO.setIsPro(player.getIsPro());
        return playerDTO;
    }

    private Player mapToEntity(final PlayerDTO playerDTO, final Player player) {
        player.setFirstname(playerDTO.getFirstname());
        player.setLastname(playerDTO.getLastname());
        player.setPdgaNumber(playerDTO.getPdgaNumber());
        player.setSdaNumber(playerDTO.getSdaNumber());
        player.setSwisstourLicense(playerDTO.getSwisstourLicense());
        player.setIsPro(playerDTO.getIsPro());
        return player;
    }

    public boolean pdgaNumberExists(final Long pdgaNumber) {
        return playerRepository.existsByPdgaNumber(pdgaNumber);
    }

    public boolean sdaNumberExists(final Long sdaNumber) {
        return playerRepository.existsBySdaNumber(sdaNumber);
    }

    public boolean nameExists(final String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        String[] names = fullName.trim().split("\\s+");
        if (names.length < 2) {
            return false;
        }
        NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
        return playerRepository.existsByFirstnameAndLastname(
                nameInfo.getFirstName(), nameInfo.getLastName());
    }

    public PlayerDTO findByPdgaNumber(Long pdgaNumber) {
        Player player =
                playerRepository
                        .findByPdgaNumber(pdgaNumber)
                        .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public PlayerDTO findByName(String fullName) {
        String[] names = fullName.trim().split("\\s+");
        NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
        Player player =
                playerRepository
                        .findByFirstnameAndLastname(nameInfo.getFirstName(), nameInfo.getLastName())
                        .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public List<PlayerEventsDTO> getPlayerEvents(final Long id) {
        return tournamentRepository.findTournamentsWithEventAndRoundsByPlayerId(id).stream()
                .filter(t -> t.getEvent().getStartDate() != null)
                .sorted(
                        Comparator.comparing(Tournament::getDivision)
                                .thenComparing(
                                        Comparator.comparing(
                                                        (Tournament t) ->
                                                                t.getEvent().getStartDate())
                                                .reversed()))
                .map(
                        t -> {
                            Event e = t.getEvent();
                            List<RoundDTOsmall> rounds =
                                    t.getRounds().stream()
                                            .map(
                                                    r ->
                                                            new RoundDTOsmall(
                                                                    r.getRoundNumber(),
                                                                    r.getRating(),
                                                                    r.getScore()))
                                            .toList();
                            return new PlayerEventsDTO(
                                    e.getId(),
                                    e.getEventId(),
                                    e.getName(),
                                    e.getDisplayName(),
                                    e.getTier(),
                                    e.getYear(),
                                    e.getCity(),
                                    e.getCountry(),
                                    e.getNumberPlayers(),
                                    e.getPoints(),
                                    e.getPurse(),
                                    e.getIsChampionship(),
                                    e.getIsSwisstour(),
                                    e.getHasResults(),
                                    e.getInfoLink(),
                                    e.getRegistrationLink(),
                                    e.getRegistrationStart(),
                                    e.getSwisstourType(),
                                    e.getStartDate(),
                                    e.getEndDate(),
                                    t.getDivision(),
                                    t.getPlace(),
                                    t.getRating(),
                                    t.getPrize(),
                                    t.getScore(),
                                    t.getPoints(),
                                    rounds);
                        })
                .toList();
    }
}

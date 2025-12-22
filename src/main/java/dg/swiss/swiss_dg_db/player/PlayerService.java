package dg.swiss.swiss_dg_db.player;

import dg.swiss.swiss_dg_db.event.EventRepository;
import dg.swiss.swiss_dg_db.event.PlayerEventsDTO;
import dg.swiss.swiss_dg_db.events.BeforeDeletePlayer;
import dg.swiss.swiss_dg_db.round.RoundDTO;
import dg.swiss.swiss_dg_db.round.RoundDTOsmall;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import dg.swiss.swiss_dg_db.scrape.PlayerDetails;
import dg.swiss.swiss_dg_db.util.NotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher publisher;
    private final PlayerDetails playerDetails;
    private final EventRepository eventRepository;

    public PlayerService(final PlayerRepository playerRepository,
                         final ApplicationEventPublisher publisher,
                         final EventRepository eventRepository) {
        this.playerRepository = playerRepository;
        this.publisher = publisher;
        this.playerDetails = new PlayerDetails();
        this.eventRepository = eventRepository;
    }

    public List<PlayerDTO> findAll() {
        final List<Player> players = playerRepository.findAll(Sort.by("id"));
        return players.stream()
                .map(player -> mapToDTO(player, new PlayerDTO()))
                .toList();
    }

    public PlayerDTO get(final Long id) {
        return playerRepository.findById(id)
                .map(player -> mapToDTO(player, new PlayerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public PlayerDTO addDetails(PlayerDTO playerDTO) throws IOException {
        if (playerDTO.getPdgaNumber() != null) {
            playerDetails.scrapePlayerInfo(playerDTO.getPdgaNumber());
            playerDTO.setFirstname(playerDetails.getFirstname());
            playerDTO.setLastname(playerDetails.getLastname());
            playerDTO.setIsPro(playerDetails.getIsPro());
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
        final Player player = playerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(playerDTO, player);
        playerRepository.save(player);
    }

    @Transactional
    public void delete(final Long id) {
        final Player player = playerRepository.findById(id)
                .orElseThrow(NotFoundException::new);

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
        return playerRepository.existsByFirstnameAndLastname(nameInfo.getFirstName(), nameInfo.getLastName());
    }

    public PlayerDTO findByPdgaNumber(Long pdgaNumber) {
        Player player = playerRepository.findByPdgaNumber(pdgaNumber)
                .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public PlayerDTO findByName(String fullName) {
        String[] names = fullName.trim().split("\\s+");
        NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
        Player player = playerRepository.findByFirstnameAndLastname(nameInfo.getFirstName(), nameInfo.getLastName())
                .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public List<PlayerEventsDTO> getPlayerEvents(final Long id) {
        List<Object[]> rawEvents = eventRepository.findRawEventsFromPlayer(id);
        Map<Long, PlayerEventsDTO> eventMap = new HashMap<>();

        for (Object[] raw : rawEvents) {
            Long eventId = (Long) raw[0];
            String name = (String) raw[1];
            String displayName = (String) raw[2];
            String tier = (String) raw[3];
            LocalDate date = (LocalDate) raw[4];
            int year = (int) raw[5];
            int numberDays = (int) raw[6];
            String city = (String) raw[7];
            String country = (String) raw[8];
            int numberPlayers = (int) raw[9];
            int points = (int) raw[10];
            double purse = (double) raw[11];
            boolean isChampionship = (boolean) raw[12];
            boolean isSwisstour = (boolean) raw[13];
            boolean hasResults = (boolean) raw[14];
            String division = (String) raw[15];
            int tournamentPlace = (int) raw[16];
            Integer tournamentRating = raw[17] != null ? (Integer) raw[17] : null; // Nullable
            double tournamentPrize = raw[18] != null ? (double) raw[18] : 0;
            int tournamentScore = raw[19] != null ? (int) raw[19] : 0;
            double tournamentPoints = (double) raw[20];

            if (!eventMap.containsKey(eventId)) {
                List<RoundDTOsmall> rounds = new ArrayList<>();
                // Create new PlayerEventsDTO with common fields
                PlayerEventsDTO eventDTO = new PlayerEventsDTO(eventId, name, displayName, tier, date, year,
                        numberDays, city, country, numberPlayers, points, purse,
                        isChampionship, isSwisstour, hasResults, division,
                        tournamentPlace, tournamentRating, tournamentPrize,
                        tournamentScore, tournamentPoints, rounds);
                eventMap.put(eventId, eventDTO);
            }

            // Extract round information
            int roundNumber = raw[21] != null ? (int) raw[21] : 0;
            int roundRating = raw[22] != null ? (int) raw[22] : 0;
            int roundScore = raw[23] != null ? (int) raw[23] : 0;
            RoundDTOsmall round = new RoundDTOsmall(roundNumber, roundRating, roundScore);
            eventMap.get(eventId).getRounds().add(round);
        }

        return new ArrayList<>(eventMap.values());
    }
}

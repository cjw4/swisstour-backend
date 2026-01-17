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
            Long pk = (Long) raw[0];
            Long eventId = (Long) raw[1];
            String name = (String) raw[2];
            String displayName = (String) raw[3];
            String tier = (String) raw[4];
            int year = (int) raw[5];
            String city = (String) raw[6];
            String country = (String) raw[7];
            int numberPlayers = (int) raw[8];
            int points = (int) raw[9];
            double purse = raw[10] != null ? (double) raw[10] : 0;
            boolean isChampionship = (boolean) raw[11];
            boolean isSwisstour = (boolean) raw[12];
            boolean hasResults = (boolean) raw[13];
            String infoLink = (String) raw[14];
            String registrationLink = (String) raw[15];
            LocalDate registrationStart = (LocalDate) raw[16];
            String swisstourType = (String) raw[17];
            LocalDate startDate = (LocalDate) raw[18];
            LocalDate endDate = (LocalDate) raw[19];
            String division = (String) raw[20];
            int tournamentPlace = (int) raw[21];
            Integer tournamentRating = raw[22] != null ? (Integer) raw[22] : null;
            double tournamentPrize = raw[23] != null ? (double) raw[23] : 0;
            int tournamentScore = raw[24] != null ? (int) raw[24] : 0;
            double tournamentPoints = (double) raw[25];

            if (!eventMap.containsKey(pk)) {
                List<RoundDTOsmall> rounds = new ArrayList<>();
                // Create new PlayerEventsDTO with common fields
                PlayerEventsDTO eventDTO = new PlayerEventsDTO(pk, eventId, name, displayName, tier, year,
                        city, country, numberPlayers, points, purse,
                        isChampionship, isSwisstour, hasResults, infoLink, registrationLink,
                        registrationStart, swisstourType, startDate, endDate, division,
                        tournamentPlace, tournamentRating, tournamentPrize,
                        tournamentScore, tournamentPoints, rounds);
                eventMap.put(pk, eventDTO);
            }

            // Extract round information
            int roundNumber = raw[26] != null ? (int) raw[26] : 0;
            int roundRating = raw[27] != null ? (int) raw[27] : 0;
            int roundScore = raw[28] != null ? (int) raw[28] : 0;
            RoundDTOsmall round = new RoundDTOsmall(roundNumber, roundRating, roundScore);
            eventMap.get(pk).getRounds().add(round);
        }

        return new ArrayList<>(eventMap.values());
    }
}

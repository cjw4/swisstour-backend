package dg.swiss.swiss_dg_db.player;

import static java.lang.Long.parseLong;

import dg.swiss.swiss_dg_db.event.Event;
import dg.swiss.swiss_dg_db.event.PlayerEventsDTO;
import dg.swiss.swiss_dg_db.events.BeforeDeletePlayer;
import dg.swiss.swiss_dg_db.player.exceptions.GoogleSheetUnavailableException;
import dg.swiss.swiss_dg_db.round.RoundDTOsmall;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import dg.swiss.swiss_dg_db.scrape.PlayerDetails;
import dg.swiss.swiss_dg_db.tournament.Tournament;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import dg.swiss.swiss_dg_db.util.NotFoundException;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher publisher;
    private final PlayerDetails playerDetails;
    private final TournamentRepository tournamentRepository;

    @Value("${app.google-sheet.csv-url}")
    private String csvExportUrl;

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

    public List<PlayerDTO> addPlayersFromGoogleSheet() throws IOException, InterruptedException {
        // Collect PlayerDTOs from Google Sheet and DB
        List<PlayerDTO> playersFromSheet = readGoogleSheet();
        List<PlayerDTO> playersFromDB = findAll();

        // Get PlayerDTOs mapped by SDA number
        Map<Long, PlayerDTO> sheetBySda =
                playersFromSheet.stream()
                        .filter(p -> p.getSdaNumber() != null)
                        .collect(Collectors.toMap(PlayerDTO::getSdaNumber, p -> p));

        // Get PlayerDTOs mapped by PDGA number
        Map<Long, PlayerDTO> sheetByPdga =
                playersFromSheet.stream()
                        .filter(p -> p.getPdgaNumber() != null)
                        .collect(Collectors.toMap(PlayerDTO::getPdgaNumber, p -> p));

        // Get PlayerDTOs mapped by name
        Map<String, PlayerDTO> sheetByName =
                playersFromSheet.stream()
                        .collect(
                                Collectors.toMap(
                                        p ->
                                                (p.getFirstname() + " " + p.getLastname())
                                                        .toLowerCase(),
                                        p -> p));

        Set<Long> matchedSheetSdaNumbers = new HashSet<>();
        List<PlayerDTO> updatedPlayers = new ArrayList<>();

        // Go through the PlayerDTOs from the DB and check for matches to the PlayerDTOs from Google
        // Sheet, first by SDA number, PDGA number and name
        for (PlayerDTO player : playersFromDB) {
            PlayerDTO match = null;

            // 1. If playerDTO from DB has SDA number, look for it in the SDA number map and return
            // its mapped PlayerDTO as match if the SDA number in the map exists
            if (player.getSdaNumber() != null) {
                match = sheetBySda.get(player.getSdaNumber());
                // Handle edge case that player has no PDGA number and name has changed in sheet
                if (match != null && match.getPdgaNumber() == null) {
                    player.setFirstname(match.getFirstname());
                    player.setLastname(match.getLastname());
                }
            }

            // 2. If match has yet to be found and the playerDTO from DB has a PDGA number, look for
            // it in the PDGA number map and return its mapped PlayerDTO as match if the PDGA number
            // in the map exists. Set the PlayerDTO from DB SDA number to that from the Google
            // sheet.
            if (match == null && player.getPdgaNumber() != null) {
                match = sheetByPdga.get(player.getPdgaNumber());
                if (match != null) {
                    player.setSdaNumber(match.getSdaNumber());
                }
            }

            // 3. If no match has yet to be found and the PlayerDTO from DB does not have a SDA
            // Number or PDGA Number, a match of name in the sheet is attempted.
            if (match == null && player.getSdaNumber() == null && player.getPdgaNumber() == null) {
                String fullName =
                        (player.getFirstname() + " " + player.getLastname()).toLowerCase();
                match = sheetByName.get(fullName);
                if (match != null) {
                    player.setSdaNumber(match.getSdaNumber());
                }
            }

            // If a match through any of the above methods was found, then set the swisstourLicense
            // of the PlayerDTO from DB to true, otherwise set it to false
            player.setSwisstourLicense(match != null);

            // Put all matched players SDA numbers into a set
            if (match != null) {
                matchedSheetSdaNumbers.add(match.getSdaNumber());
            }

            // Update the player in the database (with new swisstourLicense boolean and SDA number,
            // if was applicable) and add the PlayerDTO to the list of updatedPlayers (returned
            // object).
            update(player.getId(), player);
            updatedPlayers.add(player);

            // Also get latest information (i.e. name and isPro from PDGA website) -> not
            // implemented currently due to speed.
            //            PlayerDTO withDetails = addDetails(player);
            //            update(withDetails.getId(), withDetails);
            //            // Avoid 429 response
            //            if (withDetails.getPdgaNumber() != null) {
            //                Thread.sleep(2000);
            //            }
            //            updatedPlayers.add(withDetails);
        }

        // If the player from the Google sheet was not found in the DB, create it.
        List<PlayerDTO> createdPlayers = new ArrayList<>();
        for (PlayerDTO player : playersFromSheet) {
            if (!matchedSheetSdaNumbers.contains(player.getSdaNumber())) {
                PlayerDTO withDetails = addDetails(player);
                Long id = create(withDetails);
                createdPlayers.add(get(id));
            }
        }

        List<PlayerDTO> allPlayers = new ArrayList<>(updatedPlayers);
        allPlayers.addAll(createdPlayers);
        return allPlayers;
    }

    public List<PlayerDTO> readGoogleSheet() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(new URL(csvExportUrl).openStream()))) {
            return parseGoogleSheetCsv(reader);
        } catch (IOException e) {
            throw new GoogleSheetUnavailableException(
                    "Failed to fetch Google Sheet CSV from: " + csvExportUrl, e);
        }
    }

    List<PlayerDTO> parseGoogleSheetCsv(BufferedReader reader) throws IOException {
        List<PlayerDTO> players = new ArrayList<>();

        reader.readLine(); // Skip header row (SDA, PDGA, Nachname, Vorname)

        String line;
        while ((line = reader.readLine()) != null) {

            // Stop at first blank row
            if (line.isBlank()) break;

            String[] cols = line.split(",", -1);

            if (cols.length < 4) {
                System.err.println("Skipping malformed row: " + line);
                continue;
            }

            try {
                Long sdaNumber = parseLong(cols[0].replaceAll("[^0-9]", ""));
                Long pdgaNumber = cols[1].isBlank() ? null : parseLong(cols[1]);
                String firstName = cols[3].trim();
                String lastName = cols[2].trim();

                players.add(
                        PlayerDTO.builder()
                                .firstname(firstName)
                                .lastname(lastName)
                                .pdgaNumber(pdgaNumber)
                                .sdaNumber(sdaNumber)
                                .swisstourLicense(true)
                                .build());

            } catch (Exception e) {
                System.err.println(
                        "Skipping row due to parse error: " + line + " — " + e.getMessage());
            }
        }

        return players;
    }
}

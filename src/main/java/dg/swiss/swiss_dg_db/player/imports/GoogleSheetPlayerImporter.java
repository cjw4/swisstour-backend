package dg.swiss.swiss_dg_db.player.imports;

import static java.lang.Long.parseLong;

import dg.swiss.swiss_dg_db.player.PlayerDTO;
import dg.swiss.swiss_dg_db.player.PlayerService;
import dg.swiss.swiss_dg_db.player.exceptions.GoogleSheetUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleSheetPlayerImporter {

    private final PlayerService playerService;

    @Value("${app.google-sheet.csv-url}")
    private String csvExportUrl;

    public List<PlayerDTO> addPlayersFromGoogleSheet() throws IOException, InterruptedException {
        // Collect PlayerDTOs from Google Sheet and DB
        List<PlayerDTO> playersFromSheet = readGoogleSheet();
        List<PlayerDTO> playersFromDB = playerService.findAll();

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

            // 3. If no match has yet been found and the PlayerDTO from DB does not have an SDA
            // number, a match by name in the sheet is attempted. This also covers players that
            // already have a PDGA number in the DB but whose sheet row has no (or the same) PDGA
            // number, so that the SDA number is added to the existing player instead of creating a
            // new one. A sheet row with a different PDGA number is a different person.
            if (match == null && player.getSdaNumber() == null) {
                String fullName =
                        (player.getFirstname() + " " + player.getLastname()).toLowerCase();
                PlayerDTO nameMatch = sheetByName.get(fullName);
                if (nameMatch != null
                        && (nameMatch.getPdgaNumber() == null
                                || nameMatch.getPdgaNumber().equals(player.getPdgaNumber())
                                || player.getPdgaNumber() == null)) {
                    match = nameMatch;
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
            playerService.update(player.getId(), player);
            updatedPlayers.add(player);

            // Also get latest information (i.e. name and isPro from PDGA website) -> not
            // implemented currently due to speed.
            //            PlayerDTO withDetails = playerService.addDetails(player);
            //            playerService.update(withDetails.getId(), withDetails);
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
                PlayerDTO withDetails = playerService.addDetails(player);
                Long id = playerService.create(withDetails);
                createdPlayers.add(playerService.get(id));
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

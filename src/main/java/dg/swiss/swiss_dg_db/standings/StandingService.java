package dg.swiss.swiss_dg_db.standings;

import dg.swiss.swiss_dg_db.tournament.TournamentPointsDTO;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandingService {
    private final TournamentRepository tournamentRepository;

    public StandingService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public List<StandingDTO> getStandings(String division) {
        List<TournamentPointsDTO> tournamentPointsDTOs = tournamentRepository.findTournamentPointsByDivision(division);
        List<StandingDTO> standingDTOs = tournamentPointsDTOs.stream()
                // only include swisstour events
                .filter(TournamentPointsDTO::getIsSwisstour)
                // only include players with swisstour license
                .filter(TournamentPointsDTO::getSwisstourLicense)
                // turn the TournamentPointsDTOs into StandingDTOs
                .collect(Collectors.groupingBy(TournamentPointsDTO::getPlayerId))
                .entrySet().stream()
                .map(entry -> {
                    Long playerId = entry.getKey();
                    List<EventPointsDTO> eventPointsDTOs = entry.getValue().stream()
                            .map(event ->
                                    new EventPointsDTO(event.getEventId(), event.getPoints(), false))
                            .collect(Collectors.toList());

                    // Sort events by points in descending order
                    List<EventPointsDTO> sortedEventPointsDTOs = eventPointsDTOs.stream()
                            .sorted(Comparator.comparing(EventPointsDTO::getPoints).reversed())
                            .toList();

                    // Mark top 7 events as final
                    for (int i = 0; i < Math.min(7, sortedEventPointsDTOs.size()); i++) {
                        sortedEventPointsDTOs.get(i).setIncluded(true);
                    }

                    // Calculate the total points
                    Double totalPoints = eventPointsDTOs.stream()
                            .filter(EventPointsDTO::isIncluded)
                            .mapToDouble(EventPointsDTO::getPoints)
                            .sum();
                    return new StandingDTO(playerId, eventPointsDTOs, totalPoints, 0);
                }).toList();

        return calculateRankings(standingDTOs);
    }

    private List<StandingDTO> calculateRankings(List<StandingDTO> standingDTOs) {
        // Sort events by total points in descending order
        List<StandingDTO> rankedStandingDTOs = standingDTOs.stream()
                .sorted(Comparator.comparing(StandingDTO::getTotalPoints).reversed())
                .toList();

        // Calculate ranks with handling for tied points
        List<StandingDTO> finalRankedStandingDTOs = new ArrayList<>();
        int currentRank = 1;
        Double previousTotal = rankedStandingDTOs.isEmpty() ? 0 : rankedStandingDTOs.getFirst().getTotalPoints();

        for (int i = 0; i < rankedStandingDTOs.size(); i++) {
            StandingDTO standingDTO = rankedStandingDTOs.get(i);

            // Assign rank, keeping same rank for same total points
            if (i > 0 && standingDTO.getTotalPoints() < previousTotal) {
                currentRank = i + 1;
            }

            finalRankedStandingDTOs.add(new StandingDTO(
                    standingDTO.getPlayerId(),
                    standingDTO.getEventPoints(),
                    standingDTO.getTotalPoints(),
                    currentRank
            ));

            previousTotal = standingDTO.getTotalPoints();
        }

        return finalRankedStandingDTOs;
    }
}

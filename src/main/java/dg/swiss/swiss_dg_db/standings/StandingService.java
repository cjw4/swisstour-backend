package dg.swiss.swiss_dg_db.standings;

import dg.swiss.swiss_dg_db.tournament.Tournament;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StandingService {
    private final TournamentRepository tournamentRepository;

    public List<StandingDTO> getStandings(String division, Integer year) {
        StandingsCalculator calculator = StandingsCalculatorFactory.getCalculator(year);
        if (calculator == null) {
            return List.of(new StandingDTO(null, List.of(), 0.0, -1));
        }

        List<Tournament> tournaments = tournamentRepository.findTournamentsByDivision(division);
        List<StandingDTO> standingDTOs =
                tournaments.stream()
                        // only include swisstour events
                        .filter(t -> t.getEvent().getIsSwisstour())
                        // only include players with swisstour license
                        .filter(t -> t.getPlayer().getSwisstourLicense())
                        // only include from the particular year
                        .filter(t -> Objects.equals(t.getEvent().getYear(), year))
                        // turn the Tournaments into StandingDTOs
                        .collect(Collectors.groupingBy(t -> t.getPlayer().getId()))
                        .entrySet()
                        .stream()
                        .map(
                                entry -> {
                                    Long playerId = entry.getKey();
                                    List<EventPointsDTO> eventPointsDTOs =
                                            entry.getValue().stream()
                                                    .map(
                                                            t ->
                                                                    new EventPointsDTO(
                                                                            t.getEvent()
                                                                                    .getEventId(),
                                                                            t.getPoints(),
                                                                            t.getEvent()
                                                                                    .getPoints(),
                                                                            t.getEvent()
                                                                                    .getIsChampionship(),
                                                                            false))
                                                    .collect(Collectors.toList());

                                    Double totalPoints = calculator.calculate(eventPointsDTOs);
                                    return new StandingDTO(
                                            playerId, eventPointsDTOs, totalPoints, 0);
                                })
                        .toList();

        return calculateRankings(standingDTOs);
    }

    private List<StandingDTO> calculateRankings(List<StandingDTO> standingDTOs) {
        // Sort events by total points in descending order
        List<StandingDTO> rankedStandingDTOs =
                standingDTOs.stream()
                        .sorted(Comparator.comparing(StandingDTO::getTotalPoints).reversed())
                        .toList();

        // Calculate ranks with handling for tied points
        List<StandingDTO> finalRankedStandingDTOs = new ArrayList<>();
        int currentRank = 1;
        Double previousTotal =
                rankedStandingDTOs.isEmpty() ? 0 : rankedStandingDTOs.getFirst().getTotalPoints();

        for (int i = 0; i < rankedStandingDTOs.size(); i++) {
            StandingDTO standingDTO = rankedStandingDTOs.get(i);

            // Assign rank, keeping same rank for same total points
            if (i > 0 && standingDTO.getTotalPoints() < previousTotal) {
                currentRank = i + 1;
            }

            finalRankedStandingDTOs.add(
                    new StandingDTO(
                            standingDTO.getPlayerId(),
                            standingDTO.getEventPoints(),
                            standingDTO.getTotalPoints(),
                            currentRank));

            previousTotal = standingDTO.getTotalPoints();
        }

        return finalRankedStandingDTOs;
    }
}

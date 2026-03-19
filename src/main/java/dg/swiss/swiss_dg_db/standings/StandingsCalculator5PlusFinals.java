package dg.swiss.swiss_dg_db.standings;

import java.util.Comparator;
import java.util.List;

public class StandingsCalculator5PlusFinals implements StandingsCalculator {

    @Override
    public double calculate(List<EventPointsDTO> eventPoints) {
        // Swisstour final events (250 pts, not championship) are always included
        List<EventPointsDTO> swisstourFinalEvents =
                eventPoints.stream()
                        .filter(e -> e.getEventPoints() == 250 && !e.isChampionship())
                        .toList();
        swisstourFinalEvents.forEach(e -> e.setIncluded(true));

        // Top 5 regular events by points descending are included
        List<EventPointsDTO> topRegularEvents =
                eventPoints.stream()
                        .filter(e -> !(e.getEventPoints() == 250 && !e.isChampionship()))
                        .sorted(Comparator.comparing(EventPointsDTO::getPoints).reversed())
                        .limit(5)
                        .toList();
        topRegularEvents.forEach(e -> e.setIncluded(true));

        return eventPoints.stream()
                .filter(EventPointsDTO::isIncluded)
                .mapToDouble(EventPointsDTO::getPoints)
                .sum();
    }
}

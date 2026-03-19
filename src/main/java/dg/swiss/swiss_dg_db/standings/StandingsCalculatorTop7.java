package dg.swiss.swiss_dg_db.standings;

import java.util.Comparator;
import java.util.List;

public class StandingsCalculatorTop7 implements StandingsCalculator {

    @Override
    public double calculate(List<EventPointsDTO> eventPoints) {
        List<EventPointsDTO> top7 =
                eventPoints.stream()
                        .sorted(Comparator.comparing(EventPointsDTO::getPoints).reversed())
                        .limit(7)
                        .toList();
        top7.forEach(e -> e.setIncluded(true));

        return top7.stream().mapToDouble(EventPointsDTO::getPoints).sum();
    }
}

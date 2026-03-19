package dg.swiss.swiss_dg_db.standings;

import java.util.List;

public interface StandingsCalculator {
    double calculate(List<EventPointsDTO> eventPoints);
}

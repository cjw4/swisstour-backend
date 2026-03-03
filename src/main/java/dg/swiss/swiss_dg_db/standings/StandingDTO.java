package dg.swiss.swiss_dg_db.standings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class StandingDTO {
    private Long playerId;
    private List<EventPointsDTO> eventPoints;
    private Double totalPoints;
    private Integer rank;
}

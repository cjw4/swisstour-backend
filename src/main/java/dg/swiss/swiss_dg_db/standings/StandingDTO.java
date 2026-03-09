package dg.swiss.swiss_dg_db.standings;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StandingDTO {
    private Long playerId;
    private List<EventPointsDTO> eventPoints;
    private Double totalPoints;
    private Integer rank;
}

package dg.swiss.swiss_dg_db.standings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class EventPointsDTO {
    private Long eventId;
    private Double points;
    private Integer eventPoints;
    private boolean isChampionship;
    private boolean isIncluded;
}

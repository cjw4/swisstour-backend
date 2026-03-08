package dg.swiss.swiss_dg_db.event;

import dg.swiss.swiss_dg_db.round.RoundDTOsmall;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PlayerEventsDTO {
    private Long id;
    private Long eventId;
    private String name;
    private String displayName;
    private String tier;
    private Integer year;
    private String city;
    private String country;
    private Integer numberPlayers;
    private Integer points;
    private Double purse;
    private Boolean isChampionship;
    private Boolean isSwisstour;
    private Boolean hasResults;
    private String infoLink;
    private String registrationLink;
    private LocalDate registrationStart;
    private String swisstourType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String division;
    private Integer tournamentPlace;
    private Integer tournamentRating;
    private Double tournamentPrize;
    private Integer tournamentScore;
    private Double tournamentPoints;
    private List<RoundDTOsmall> rounds;
}

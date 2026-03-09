package dg.swiss.swiss_dg_db.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventDTO {

    private Long id;

    private Long eventId;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String displayName;

    @Size(max = 255)
    private String tier;

    private Integer year;

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String country;

    private Integer numberPlayers;

    @NotNull private Integer points;

    private Double purse;

    @NotNull
    @JsonProperty("isChampionship")
    private Boolean isChampionship;

    @NotNull
    @JsonProperty("isSwisstour")
    private Boolean isSwisstour;

    @JsonProperty("hasResults")
    private Boolean hasResults;

    @Size(max = 500)
    private String infoLink;

    @Size(max = 500)
    private String registrationLink;

    private LocalDate registrationStart;

    @Size(max = 50)
    private String swisstourType;

    private LocalDate startDate;

    private LocalDate endDate;
}

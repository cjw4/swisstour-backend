package dg.swiss.swiss_dg_db.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class EventDTO {

    private Long id;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String displayName;

    @Size(max = 255)
    private String tier;

    private LocalDate date;

    private Integer numberDays;

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String country;

    private Integer numberPlayers;

    @NotNull
    private Integer points;

    private Double purse;

    @NotNull
    @JsonProperty("isChampionship")
    private Boolean isChampionship;

    @NotNull
    @JsonProperty("isSwisstour")
    private Boolean isSwisstour;

    @JsonProperty("hasResults")
    private Boolean hasResults;

}

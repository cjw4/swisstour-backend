package dg.swiss.swiss_dg_db.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PlayerDTO {

    private Long id;

    @Size(max = 255)
    private String firstname;

    @Size(max = 255)
    private String lastname;

    @PlayerPdgaNumberUnique
    private Long pdgaNumber;

    @PlayerSdaNumberUnique
    private Long sdaNumber;

    @NotNull
    private Boolean swisstourLicense;

    @JsonProperty("isPro")
    private Boolean isPro;

}

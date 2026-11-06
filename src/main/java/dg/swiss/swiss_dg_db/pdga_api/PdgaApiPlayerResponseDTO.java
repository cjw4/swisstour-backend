package dg.swiss.swiss_dg_db.pdga_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class PdgaApiPlayerResponseDTO {
    private String first_name;
    private String last_name;
    private Long pdga_number;
    private Integer rating;
    private Integer year;

    @JsonProperty("class")
    private String playerClass;

    private String gender;
    private String division_name;
    private String division_code;
    private String country;
    private Integer tournaments;
    private Integer rating_rounds_used;
    private Long points;
    private Long prize;
    private String last_modified;
}

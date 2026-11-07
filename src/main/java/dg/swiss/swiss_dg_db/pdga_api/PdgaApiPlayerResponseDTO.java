package dg.swiss.swiss_dg_db.pdga_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private BigDecimal points;
    private BigDecimal prize;
    private LocalDate last_modified;
}

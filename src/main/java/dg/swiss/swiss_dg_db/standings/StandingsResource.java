package dg.swiss.swiss_dg_db.standings;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@Tag(name = "Standings")
public class StandingsResource {

    private final StandingService standingService;

    public StandingsResource(StandingService standingService) {
        this.standingService = standingService;
    }

    @GetMapping("/{year}/{division}")
    public ResponseEntity<List<StandingDTO>> getStandings(@PathVariable(name = "division") final String division,
                                                          @PathVariable(name = "year") final Integer year) {
        return ResponseEntity.ok(standingService.getStandings(division, year));
    }
}

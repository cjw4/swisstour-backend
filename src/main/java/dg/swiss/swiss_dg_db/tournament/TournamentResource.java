package dg.swiss.swiss_dg_db.tournament;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/tournaments", produces = MediaType.APPLICATION_JSON_VALUE)
public class TournamentResource {

    private final TournamentService tournamentService;

    public TournamentResource(final TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public ResponseEntity<List<TournamentDTO>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentDTO> getTournament(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(tournamentService.get(id));
    }

    @PostMapping
    public ResponseEntity<Long> createTournament(
            @RequestBody @Valid final TournamentDTO tournamentDTO) {
        final Long createdId = tournamentService.create(tournamentDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updateTournament(
            @PathVariable(name = "id") final Long id,
            @RequestBody @Valid final TournamentDTO tournamentDTO) {
        tournamentService.update(id, tournamentDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable(name = "id") final Long id) {
        tournamentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

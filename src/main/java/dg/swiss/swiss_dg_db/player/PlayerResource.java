package dg.swiss.swiss_dg_db.player;

import dg.swiss.swiss_dg_db.event.PlayerEventsDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(value = "/api/players", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Players")
public class PlayerResource {

    private final PlayerService playerService;

    public PlayerResource(final PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        return ResponseEntity.ok(playerService.findAll());
    }

    @Operation(description = "Get player by id")
    @GetMapping("/{id}")
    public ResponseEntity<PlayerDTO> getPlayer(@PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(playerService.get(id));
    }

    @PostMapping
    public ResponseEntity<Long> createPlayer(@RequestBody @Valid final PlayerDTO playerDTO)
            throws IOException {
        PlayerDTO playerDTOwDetails = playerService.addDetails(playerDTO);
        final Long createdId = playerService.create(playerDTOwDetails);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Long> updatePlayer(
            @PathVariable(name = "id") final Long id, @RequestBody @Valid final PlayerDTO playerDTO)
            throws IOException {
        PlayerDTO playerDTOwDetails = playerService.addDetails(playerDTO);
        playerService.update(id, playerDTOwDetails);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable(name = "id") final Long id) {
        playerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<List<PlayerEventsDTO>> getPlayerEvents(
            @PathVariable(name = "id") final Long id) {
        return ResponseEntity.ok(playerService.getPlayerEvents(id));
    }
}

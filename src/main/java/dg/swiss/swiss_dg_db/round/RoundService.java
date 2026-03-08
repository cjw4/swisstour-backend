package dg.swiss.swiss_dg_db.round;

import dg.swiss.swiss_dg_db.events.BeforeDeleteTournament;
import dg.swiss.swiss_dg_db.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class RoundService {

    private final RoundRepository roundRepository;

    public RoundService(final RoundRepository roundRepository) {
        this.roundRepository = roundRepository;
    }

    @EventListener(BeforeDeleteTournament.class)
    public void on(final BeforeDeleteTournament event) {
        final ReferencedException referencedException = new ReferencedException();
        final Round tournamentRound = roundRepository.findFirstByTournamentId(event.getId());
        if (tournamentRound != null) {
            referencedException.setKey("tournament.round.tournament.referenced");
            referencedException.addParam(tournamentRound.getId());
            throw referencedException;
        }
    }
}

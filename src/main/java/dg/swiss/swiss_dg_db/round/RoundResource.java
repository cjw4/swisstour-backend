package dg.swiss.swiss_dg_db.round;

import dg.swiss.swiss_dg_db.tournament.Tournament;
import dg.swiss.swiss_dg_db.tournament.TournamentRepository;
import dg.swiss.swiss_dg_db.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RoundResource {
    private final TournamentRepository tournamentRepository;
    private final RoundRepository roundRepository;
    private static final Logger logger = LoggerFactory.getLogger(RoundResource.class);

    public RoundResource(
            TournamentRepository tournamentRepository, RoundRepository roundRepository) {
        this.tournamentRepository = tournamentRepository;
        this.roundRepository = roundRepository;
    }

    public void createRound(final RoundDTO roundDTO) {
        final Round round = new Round();
        mapToEntity(roundDTO, round);
        logger.info("Add round {}", round.getRoundNumber());
        roundRepository.save(round);
    }

    private Round mapToEntity(final RoundDTO roundDTO, final Round round) {
        round.setRoundNumber(roundDTO.getRoundNumber());
        round.setScore(roundDTO.getScore());
        round.setRating(roundDTO.getRating());
        final Tournament tournament =
                roundDTO.getTournament() == null
                        ? null
                        : tournamentRepository
                                .findById(roundDTO.getTournament())
                                .orElseThrow(() -> new NotFoundException("Tournament not found"));
        round.setTournament(tournament);
        return round;
    }
}

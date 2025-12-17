package dg.swiss.swiss_dg_db.player;

import dg.swiss.swiss_dg_db.event.EventRepository;
import dg.swiss.swiss_dg_db.event.PlayerEventsDTO;
import dg.swiss.swiss_dg_db.events.BeforeDeletePlayer;
import dg.swiss.swiss_dg_db.scrape.NameConverter;
import dg.swiss.swiss_dg_db.scrape.PlayerDetails;
import dg.swiss.swiss_dg_db.util.NotFoundException;

import java.io.IOException;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final ApplicationEventPublisher publisher;
    private final PlayerDetails playerDetails;
    private final EventRepository eventRepository;

    public PlayerService(final PlayerRepository playerRepository,
                         final ApplicationEventPublisher publisher,
                         final EventRepository eventRepository) {
        this.playerRepository = playerRepository;
        this.publisher = publisher;
        this.playerDetails = new PlayerDetails();
        this.eventRepository = eventRepository;
    }

    public List<PlayerDTO> findAll() {
        final List<Player> players = playerRepository.findAll(Sort.by("id"));
        return players.stream()
                .map(player -> mapToDTO(player, new PlayerDTO()))
                .toList();
    }

    public PlayerDTO get(final Long id) {
        return playerRepository.findById(id)
                .map(player -> mapToDTO(player, new PlayerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public PlayerDTO addDetails(PlayerDTO playerDTO) throws IOException {
        if (playerDTO.getPdgaNumber() != null) {
            playerDetails.scrapePlayerInfo(playerDTO.getPdgaNumber());
            playerDTO.setFirstname(playerDetails.getFirstname());
            playerDTO.setLastname(playerDetails.getLastname());
            playerDTO.setIsPro(playerDetails.getIsPro());
        } else {
            playerDTO.setIsPro(false);
        }
        return playerDTO;
    }

    public Long create(final PlayerDTO playerDTO) {
        final Player player = new Player();
        mapToEntity(playerDTO, player);
        return playerRepository.save(player).getId();
    }

    public void update(final Long id, final PlayerDTO playerDTO) {
        final Player player = playerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(playerDTO, player);
        playerRepository.save(player);
    }

    @Transactional
    public void delete(final Long id) {
        final Player player = playerRepository.findById(id)
                .orElseThrow(NotFoundException::new);

        // Explicitly clear relationships
        player.getTournaments().clear();
        playerRepository.save(player);

        publisher.publishEvent(new BeforeDeletePlayer(id));
        playerRepository.delete(player);
    }

    private PlayerDTO mapToDTO(final Player player, final PlayerDTO playerDTO) {
        playerDTO.setId(player.getId());
        playerDTO.setFirstname(player.getFirstname());
        playerDTO.setLastname(player.getLastname());
        playerDTO.setPdgaNumber(player.getPdgaNumber());
        playerDTO.setSdaNumber(player.getSdaNumber());
        playerDTO.setSwisstourLicense(player.getSwisstourLicense());
        playerDTO.setIsPro(player.getIsPro());
        return playerDTO;
    }

    private Player mapToEntity(final PlayerDTO playerDTO, final Player player) {
        player.setFirstname(playerDTO.getFirstname());
        player.setLastname(playerDTO.getLastname());
        player.setPdgaNumber(playerDTO.getPdgaNumber());
        player.setSdaNumber(playerDTO.getSdaNumber());
        player.setSwisstourLicense(playerDTO.getSwisstourLicense());
        player.setIsPro(playerDTO.getIsPro());
        return player;
    }

    public boolean pdgaNumberExists(final Long pdgaNumber) {
        return playerRepository.existsByPdgaNumber(pdgaNumber);
    }

    public boolean sdaNumberExists(final Long sdaNumber) {
        return playerRepository.existsBySdaNumber(sdaNumber);
    }

    public boolean nameExists(final String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return false;
        }
        String[] names = fullName.trim().split("\\s+");
        if (names.length < 2) {
            return false;
        }
        NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
        return playerRepository.existsByFirstnameAndLastname(nameInfo.getFirstName(), nameInfo.getLastName());
    }

    public PlayerDTO findByPdgaNumber(Long pdgaNumber) {
        Player player = playerRepository.findByPdgaNumber(pdgaNumber)
                .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public PlayerDTO findByName(String fullName) {
        String[] names = fullName.trim().split("\\s+");
        NameConverter.NameInfo nameInfo = NameConverter.splitName(names);
        Player player = playerRepository.findByFirstnameAndLastname(nameInfo.getFirstName(), nameInfo.getLastName())
                .orElseThrow(() -> new NotFoundException("Player not found"));
        return mapToDTO(player, new PlayerDTO());
    }

    public List<PlayerEventsDTO> getPlayerEvents(final Long id) {
        return eventRepository.findEventsFromPlayer(id);
    }
}

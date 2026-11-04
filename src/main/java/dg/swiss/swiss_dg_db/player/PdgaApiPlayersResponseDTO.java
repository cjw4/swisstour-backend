package dg.swiss.swiss_dg_db.player;

import java.util.List;
import lombok.Getter;

@Getter
public class PdgaApiPlayersResponseDTO {
    String sessionid;
    int status;
    List<PdgaApiPlayerDTO> players;
}

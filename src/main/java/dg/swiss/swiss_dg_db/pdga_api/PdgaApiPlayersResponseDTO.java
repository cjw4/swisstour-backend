package dg.swiss.swiss_dg_db.pdga_api;

import java.util.List;
import lombok.Getter;

@Getter
public class PdgaApiPlayersResponseDTO {
    String sessionid;
    int status;
    List<PdgaApiPlayerResponseDTO> players;
}

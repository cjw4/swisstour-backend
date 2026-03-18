package dg.swiss.swiss_dg_db.player.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PlayerUpdateException extends RuntimeException {

    public PlayerUpdateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

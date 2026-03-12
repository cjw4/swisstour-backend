package dg.swiss.swiss_dg_db.player.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GoogleSheetUnavailableException extends RuntimeException {

    public GoogleSheetUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

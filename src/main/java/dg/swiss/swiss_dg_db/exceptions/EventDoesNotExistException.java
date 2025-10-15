package dg.swiss.swiss_dg_db.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventDoesNotExistException extends RuntimeException {
    public EventDoesNotExistException() {
        super("The event with that PDGA event number cannot be found on the PDGA website.");
    }
}

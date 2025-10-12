package dg.swiss.swiss_dg_db.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EventAlreadyExistsException extends RuntimeException {
    public EventAlreadyExistsException() {
        super("An event with that PDGA event number already exists.");
    }
}

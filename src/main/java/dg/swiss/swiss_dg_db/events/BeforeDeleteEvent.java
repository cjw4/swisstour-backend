package dg.swiss.swiss_dg_db.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BeforeDeleteEvent {

    private Long id;
}

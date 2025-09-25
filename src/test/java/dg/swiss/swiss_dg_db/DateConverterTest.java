package dg.swiss.swiss_dg_db;

import dg.swiss.swiss_dg_db.scrape.DateConverter;
import dg.swiss.swiss_dg_db.scrape.DateConverter.DateInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateConverterTest {

    @Test
    void testConvertDate_examples() {
        // "12-Jul to 13-Jul-2025"
        DateInfo info1 = DateConverter.convertDate("12-Jul to 13-Jul-2025");
        assertEquals(LocalDate.of(2025, 7, 12), info1.getStartDate());
        assertEquals(LocalDate.of(2025, 7, 13), info1.getEndDate());
        assertEquals(2, info1.getDays());

        // "29-Jun-2025"
        DateInfo info2 = DateConverter.convertDate("29-Jun-2025");
        assertEquals(LocalDate.of(2025, 6, 29), info2.getStartDate());
        assertEquals(LocalDate.of(2025, 6, 29), info2.getEndDate());
        assertEquals(1, info2.getDays());

        // "26-Jun to 29-Jun-2025"
        DateInfo info3 = DateConverter.convertDate("26-Jun to 29-Jun-2025");
        assertEquals(LocalDate.of(2025, 6, 26), info3.getStartDate());
        assertEquals(LocalDate.of(2025, 6, 29), info3.getEndDate());
        assertEquals(4, info3.getDays());
    }
}


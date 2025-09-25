package dg.swiss.swiss_dg_db;

import dg.swiss.swiss_dg_db.scrape.LocationConverter;
import dg.swiss.swiss_dg_db.scrape.LocationConverter.LocationInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationConverterTest {

    @Test
    void testConvertLocation_examples() {
        // "Madison, WI, United States"
        LocationInfo info1 = LocationConverter.convertLocation("Madison, WI, United States");
        assertEquals("Madison", info1.getCity());
        assertEquals("WI", info1.getState());
        assertEquals("United States", info1.getCountry());

        // "Zurich, Switzerland"
        LocationInfo info2 = LocationConverter.convertLocation("Zurich, Switzerland");
        assertEquals("Zurich", info2.getCity());
        assertNull(info2.getState());
        assertEquals("Switzerland", info2.getCountry());
    }
}

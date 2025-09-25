package dg.swiss.swiss_dg_db.scrape;

import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateConverter {

    @Getter
    public static class DateInfo {
        private LocalDate startDate;
        private LocalDate endDate;
        private int days;

        public DateInfo(LocalDate startDate, LocalDate endDate, int days) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
        }
    }

    public static DateInfo convertDate(String date) {
        DateTimeFormatter formatterWithYear = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        if (date.contains(" to ")) {
            String[] parts = date.split(" to ");
            LocalDate endDate = LocalDate.parse(parts[1], formatterWithYear);
            LocalDate startDate = LocalDate.parse(parts[0] + "-" + endDate.getYear(), formatterWithYear);
            int days = (int) (ChronoUnit.DAYS.between(startDate, endDate) + 1);
            return new DateInfo(startDate, endDate, days);
        } else {
            LocalDate dateObj = LocalDate.parse(date, formatterWithYear);
            return new DateInfo(dateObj, dateObj, 1);
        }
    }
}


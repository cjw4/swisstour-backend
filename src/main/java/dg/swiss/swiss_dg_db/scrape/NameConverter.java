package dg.swiss.swiss_dg_db.scrape;

import lombok.Getter;

import java.util.Arrays;

public class NameConverter {

    @Getter
    public static class NameInfo {
        private String firstName;
        private String lastName;

        public NameInfo(String firstname, String lastname) {
            this.firstName = firstname;
            this.lastName = lastname;
        }
    }

    public static NameInfo splitName(String[] names) {
        String firstName = String.join(" ", Arrays.copyOfRange(names, 0, names.length - 1));
        String lastName = names[names.length - 1];
        return new NameInfo(firstName, lastName);
    }

}

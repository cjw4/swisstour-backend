package dg.swiss.swiss_dg_db.standings;

public class StandingsCalculatorFactory {

    public static StandingsCalculator getCalculator(Integer year) {
        return switch (year) {
            case 2025 -> new StandingsCalculatorTop7();
            case 2026 -> new StandingsCalculator5PlusFinals();
            default -> null;
        };
    }
}

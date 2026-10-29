package ProjectForJob.example.Job.util;

public final class WorkpieceCalculator {
    private static final double STEEL_DENSITY = 7850.0; // кг/м³
    private static final double PI = Math.PI;

    /**
     * Рассчитывает теоретический вес круглой трубы.
     *
     * @param outerDiameterMm наружный диаметр, мм
     * @param wallThicknessMm толщина стенки, мм
     * @param lengthMeters    длина, м
     * @return вес в кг
     */
    public static double calculateWeight(double outerDiameterMm,
                                         double wallThicknessMm,
                                         double lengthMeters) {
        if (outerDiameterMm <= 0 || wallThicknessMm <= 0 || lengthMeters <= 0) {
            return 0.0;
        }
        // Формула: M = π * ρ * S * (D - S) * L / 1_000_000
        double weight = PI * STEEL_DENSITY * wallThicknessMm
                * (outerDiameterMm - wallThicknessMm) * lengthMeters
                / 1_000_000.0;
        // Округлим до 2 знаков
        return Math.round(weight * 100.0) / 100.0;
    }
}


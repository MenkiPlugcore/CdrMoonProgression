package id.menki.cdrmoonprogression;

import java.util.Locale;
import java.util.Optional;

public enum ProgressStage {
    OVERWORLD("overworld", "Overworld"),
    NETHER("nether", "Nether");

    private final String key;
    private final String displayName;

    ProgressStage(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ProgressStage> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ProgressStage stage : values()) {
            if (stage.key.equals(normalized) || stage.name().equalsIgnoreCase(value)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}

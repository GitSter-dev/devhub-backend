package com.application.devhub.client;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ClientVersion(int major, int minor, int patch) implements Comparable<ClientVersion> {

    private static final Pattern SEMVER = Pattern.compile("^(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})(?:[-+].*)?$");
    private static final Comparator<ClientVersion> ORDER = Comparator.comparingInt(ClientVersion::major)
            .thenComparingInt(ClientVersion::minor)
            .thenComparingInt(ClientVersion::patch);

    public static Optional<ClientVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = SEMVER.matcher(value.strip());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ClientVersion(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))));
    }

    public boolean isOlderThan(ClientVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(ClientVersion other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}

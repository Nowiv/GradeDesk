package ch.gradedesk.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small SemVer 2.0 parser/comparator used for release tags. */
public record SemVer(int major, int minor, int patch, List<String> prerelease)
        implements Comparable<SemVer> {
    private static final Pattern PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");

    public SemVer {
        prerelease = List.copyOf(Objects.requireNonNull(prerelease));
    }

    public static SemVer parse(String value) {
        if (value == null) throw new IllegalArgumentException("Version is null");
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid SemVer: " + value);
        List<String> ids = new ArrayList<>();
        if (matcher.group(4) != null) {
            for (String id : matcher.group(4).split("\\.", -1)) {
                if (id.isEmpty() || (id.matches("\\d+") && id.length() > 1 && id.startsWith("0"))) {
                    throw new IllegalArgumentException("Invalid SemVer prerelease: " + value);
                }
                ids.add(id);
            }
        }
        try {
            return new SemVer(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), ids);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Version component is too large: " + value, e);
        }
    }

    @Override public int compareTo(SemVer other) {
        int c = Integer.compare(major, other.major);
        if (c == 0) c = Integer.compare(minor, other.minor);
        if (c == 0) c = Integer.compare(patch, other.patch);
        if (c != 0) return c;
        if (prerelease.isEmpty()) return other.prerelease.isEmpty() ? 0 : 1;
        if (other.prerelease.isEmpty()) return -1;
        for (int i = 0; i < Math.min(prerelease.size(), other.prerelease.size()); i++) {
            String a = prerelease.get(i), b = other.prerelease.get(i);
            boolean an = a.matches("\\d+"), bn = b.matches("\\d+");
            if (an && bn) c = compareNumeric(a, b);
            else if (an != bn) c = an ? -1 : 1;
            else c = a.compareTo(b);
            if (c != 0) return c;
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    @Override public String toString() {
        return major + "." + minor + "." + patch
                + (prerelease.isEmpty() ? "" : "-" + String.join(".", prerelease));
    }

    private static int compareNumeric(String a, String b) {
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }
}

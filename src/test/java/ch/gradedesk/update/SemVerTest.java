package ch.gradedesk.update;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SemVerTest {
    @Test void comparesNumericComponents() {
        assertTrue(SemVer.parse("1.10.0").compareTo(SemVer.parse("1.9.9")) > 0);
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.99.99")) > 0);
    }
    @Test void stableReleaseIsNewerThanPrerelease() {
        assertTrue(SemVer.parse("1.2.3").compareTo(SemVer.parse("1.2.3-rc.1")) > 0);
        assertTrue(SemVer.parse("1.2.3-rc.2").compareTo(SemVer.parse("1.2.3-rc.10")) < 0);
    }
    @Test void rejectsLeadingZeroesAndMalformedVersions() {
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("01.2.3"));
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("1.2"));
    }
}

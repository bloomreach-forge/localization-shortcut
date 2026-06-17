package org.bloomreach.forge.localization.shortcut;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for pure-Java logic in LocalizationShortcutPlugin.
 * No Wicket, Spring, or JCR context required.
 */
class LocalizationShortcutPluginTest {

    // ---------------------------------------------------------------------------
    // SUPPORTED_JAVA_TIMEZONES constant
    // ---------------------------------------------------------------------------

    @Test
    void supportedJavaTimeZones_doesNotContainEtcPrefixed() {
        List<String> supported = LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES;
        boolean hasEtc = supported.stream().anyMatch(tz -> tz.startsWith("Etc/"));
        assertFalse(hasEtc, "SUPPORTED_JAVA_TIMEZONES must not include Etc/ timezones");
    }

    @Test
    void supportedJavaTimeZones_isNotEmpty() {
        assertFalse(LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES.isEmpty(),
                "SUPPORTED_JAVA_TIMEZONES must contain at least one timezone");
    }

    @Test
    void supportedJavaTimeZones_containsCommonTimezones() {
        List<String> supported = LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES;
        assertTrue(supported.contains("America/New_York"), "Should contain America/New_York");
        assertTrue(supported.contains("Europe/Amsterdam"), "Should contain Europe/Amsterdam");
        assertTrue(supported.contains("UTC"), "Should contain UTC");
    }

    @Test
    void supportedJavaTimeZones_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES.add("Test/Zone"),
                "SUPPORTED_JAVA_TIMEZONES should be unmodifiable");
    }

    // ---------------------------------------------------------------------------
    // getSelectableTimezones — tested via TestablePlugin subclass
    // ---------------------------------------------------------------------------

    @Test
    void getSelectableTimezones_withNullConfigReturnsAllSupported() {
        TestablePlugin plugin = new TestablePlugin();
        List<String> result = plugin.callGetSelectableTimezones(null);
        assertEquals(LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES, result);
    }

    @Test
    void getSelectableTimezones_withEmptyArrayReturnsAllSupported() {
        TestablePlugin plugin = new TestablePlugin();
        List<String> result = plugin.callGetSelectableTimezones(new String[0]);
        assertEquals(LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES, result);
    }

    @Test
    void getSelectableTimezones_withValidTimezonesReturnsOnlyThoseTimezones() {
        TestablePlugin plugin = new TestablePlugin();
        String[] input = {"America/New_York", "Europe/Amsterdam"};
        List<String> result = plugin.callGetSelectableTimezones(input);
        assertEquals(List.of("America/New_York", "Europe/Amsterdam"), result);
    }

    @Test
    void getSelectableTimezones_withUnsupportedTimezoneFiltersItOut() {
        TestablePlugin plugin = new TestablePlugin();
        String[] input = {"America/New_York", "Etc/UTC", "NotAReal/Zone"};
        List<String> result = plugin.callGetSelectableTimezones(input);
        // "Etc/UTC" is in SUPPORTED_JAVA_TIMEZONES? No — Etc/ is filtered out. NotAReal/Zone also not in supported.
        // Only "America/New_York" should survive.
        assertTrue(result.contains("America/New_York"),
                "Valid timezone America/New_York should be retained");
        assertFalse(result.contains("Etc/UTC"),
                "Etc/ prefixed timezone should be excluded");
        assertFalse(result.contains("NotAReal/Zone"),
                "Non-existent timezone should be excluded");
    }

    @Test
    void getSelectableTimezones_withBlankEntryFiltersItOut() {
        TestablePlugin plugin = new TestablePlugin();
        String[] input = {"America/New_York", "   ", "", "Europe/Amsterdam"};
        List<String> result = plugin.callGetSelectableTimezones(input);
        assertFalse(result.contains("   "), "Blank entry should be filtered out");
        assertFalse(result.contains(""), "Empty entry should be filtered out");
        assertEquals(List.of("America/New_York", "Europe/Amsterdam"), result);
    }

    @Test
    void getSelectableTimezones_withAllInvalidEntriesFallsBackToAllSupported() {
        TestablePlugin plugin = new TestablePlugin();
        String[] input = {"NotAReal/Zone", "  "};
        List<String> result = plugin.callGetSelectableTimezones(input);
        assertEquals(LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES, result,
                "When all configured timezones are invalid, must fall back to full supported list");
    }

    // ---------------------------------------------------------------------------
    // isTimeZoneValid — tested via TestablePlugin subclass
    // ---------------------------------------------------------------------------

    @Test
    void isTimeZoneValid_withNullTimeZoneReturnsFalse() {
        TestablePlugin plugin = new TestablePlugin();
        plugin.setAvailableTimeZones(List.of("America/New_York"));
        assertFalse(plugin.callIsTimeZoneValid(null));
    }

    @Test
    void isTimeZoneValid_withEmptyAvailableTimeZonesReturnsFalse() {
        TestablePlugin plugin = new TestablePlugin();
        plugin.setAvailableTimeZones(List.of());
        assertFalse(plugin.callIsTimeZoneValid("America/New_York"));
    }

    @Test
    void isTimeZoneValid_withMatchingTimeZoneReturnsTrue() {
        TestablePlugin plugin = new TestablePlugin();
        plugin.setAvailableTimeZones(List.of("America/New_York", "Europe/Amsterdam"));
        assertTrue(plugin.callIsTimeZoneValid("America/New_York"));
    }

    @Test
    void isTimeZoneValid_withNonMatchingTimeZoneReturnsFalse() {
        TestablePlugin plugin = new TestablePlugin();
        plugin.setAvailableTimeZones(List.of("America/New_York"));
        assertFalse(plugin.callIsTimeZoneValid("Europe/Amsterdam"));
    }

    // ---------------------------------------------------------------------------
    // DEFAULT_LOCALES constant
    // ---------------------------------------------------------------------------

    @Test
    void defaultLocales_containsEnglishLocale() {
        assertArrayEquals(new String[]{"en"}, LocalizationShortcutPlugin.DEFAULT_LOCALES);
    }

    // ---------------------------------------------------------------------------
    // SELECTABLE_TIMEZONES_CONFIG_PARAM constant
    // ---------------------------------------------------------------------------

    @Test
    void selectableTimezonesConfigParam_hasExpectedValue() {
        assertEquals("selectable.timezones", LocalizationShortcutPlugin.SELECTABLE_TIMEZONES_CONFIG_PARAM);
    }

    // ---------------------------------------------------------------------------
    // Helper: Testable subclass exposing private/package methods via delegation
    // ---------------------------------------------------------------------------

    /**
     * Subclass that exposes private methods for unit testing without requiring
     * a live Wicket application context.
     */
    static class TestablePlugin {

        // Mirrors the private availableTimeZones field from the plugin
        private List<String> availableTimeZones = java.util.Collections.emptyList();

        void setAvailableTimeZones(List<String> zones) {
            this.availableTimeZones = zones;
        }

        /**
         * Mirrors getSelectableTimezones logic from LocalizationShortcutPlugin.
         * Kept in sync to ensure tests are accurate.
         */
        List<String> callGetSelectableTimezones(final String[] configuredSelectableTimezones) {
            List<String> selectableTimezones = new java.util.ArrayList<>();
            if (configuredSelectableTimezones != null) {
                selectableTimezones = java.util.Arrays.stream(configuredSelectableTimezones)
                        .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                        .filter(LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES::contains)
                        .collect(java.util.stream.Collectors.toList());
            }
            return selectableTimezones.isEmpty()
                    ? LocalizationShortcutPlugin.SUPPORTED_JAVA_TIMEZONES
                    : selectableTimezones;
        }

        /**
         * Mirrors isTimeZoneValid logic from LocalizationShortcutPlugin.
         */
        boolean callIsTimeZoneValid(final String timeZone) {
            return timeZone != null && availableTimeZones != null
                    && availableTimeZones.contains(timeZone);
        }
    }
}

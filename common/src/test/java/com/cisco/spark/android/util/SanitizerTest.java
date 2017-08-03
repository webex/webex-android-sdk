package com.cisco.spark.android.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SanitizerTest {

    @Test
    public void testLargeStrings() {

        Sanitizer sanitizer = new Sanitizer(true, 5);

        String small = "yes";
        String medium = "four.";
        String large = "seven.";

        assertEquals("Strings shorter than the sanitize limit shouldn't be truncated", small, sanitizer.sanitize(small));
        assertEquals("Strings with length equal to the sanitize limit shouldn't be truncated", medium, sanitizer.sanitize(medium));
        assertEquals("Strings longer than than the sanitize limit should be truncated", "seven", sanitizer.sanitize(large));
    }

    @Test
    public void testEnabled() {

        Sanitizer enabled = new Sanitizer(true);
        Sanitizer disabled = new Sanitizer(false);

        String plaintext = "\"refresh_token\":\"MzQyZTQ2ZDgtNGM4Ni00N2VhLTlmOTctN2Q4YzdjNTgwY2QxODhiNDQwZDItMDM1\"";
        String redacted = "\"refresh_token\":<redacted>";

        assertEquals("Disabled sanitizer should not change input", plaintext, disabled.sanitize(plaintext));
        assertEquals("Enabled sanitizer should redact refresh tokens", redacted, enabled.sanitize(plaintext));
    }

    @Test
    public void testSanitize() {

        Sanitizer sanitizer = new Sanitizer(true);

        List<String> tokenKeys = Arrays.asList("refresh_token", "access_token");

        for (String tokenKey : tokenKeys) {
            String token = "\"" + tokenKey + "\":\"MzQyZTQ2ZDgtNGM4Ni00N2VhLTlmOTctN2Q4YzdjNTgwY2QxODhiNDQwZDItMDM1\"";
            String tokenRedacted = "\"" + tokenKey + "\":<redacted>";
            assertEquals("Tokens must not be printed", tokenRedacted, sanitizer.sanitize(token));
        }

        List<String> userPii = Arrays.asList("displayName", "name", "email");

        for (String user : userPii) {
            String pii = "\"" + user + "\":\"This is a normal string\"";
            String redactedPii = "\"" + user + "\":<redacted>";
            assertEquals("User json elements should be redacted", redactedPii, sanitizer.sanitize(pii));
        }

        String bearerPlain = "Bearer asdfjasdfiasdfuasdfhasdfoasdfiuasdfusadf";
        String redacted = "<redacted>";

        assertEquals(redacted, sanitizer.sanitize(bearerPlain));

        String refreshPlain = "refresh_token=askljdfaiusdfiuasncqet5c134tertjdrgnc2n5t2u35tiwergk";
        String refreshRedacted = "refresh_token=<redacted>";
        assertEquals(refreshRedacted, sanitizer.sanitize(refreshPlain));

        String accessPlain = "access_token=asdklj834789e0twkedfmgsdfg-e3458345jrgb09erg7dfgjdfgodfgusdgjdgdf78";
        String accessRedacted = "access_token=<redacted>";
        assertEquals(accessRedacted, sanitizer.sanitize(accessPlain));
    }
}

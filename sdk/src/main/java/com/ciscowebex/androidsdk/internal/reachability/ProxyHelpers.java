/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.reachability;

import com.ciscowebex.androidsdk.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyHelpers {
    public enum ProxyAutheticationMethod {
        PROXY_AUTH_BASIC("Basic"),
        PROXY_AUTH_DIGEST("Digest"),
        PROXY_AUTH_NTLM("NTLM");

        private String type;

        ProxyAutheticationMethod(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static ProxyAutheticationMethod fromString(String text) {
            for (ProxyAutheticationMethod method : ProxyAutheticationMethod.values()) {
                if (method.type.equals(text)) {
                    return method;
                }
            }
            return null;
        }
    }

    public static String authenticateHeaderForType(List<String> headers, ProxyAutheticationMethod type) {
        String foundHeader = null;

        if (headers.size() == 1) {
            foundHeader = parseSingleHeader(headers.get(0), type);
        } else {
            for (String currentHeader : headers) {
                if (currentHeader.startsWith(type.getType())) {
                    foundHeader = currentHeader;
                    break;
                }
            }
        }

        return foundHeader;
    }

    public static ProxyAutheticationMethod chooseAuthenticationMethod(List<String> availableAuthenticationHeaders) {
        List<ProxyAutheticationMethod> methods = new ArrayList<>();

        if (availableAuthenticationHeaders.size() > 1) {
            for (String authenticationHeader : availableAuthenticationHeaders) {
                ProxyAutheticationMethod method = authenticationMethod(authenticationHeader);

                if (method != null) {
                    methods.add(method);
                }
            }
        } else {
            String header = availableAuthenticationHeaders.get(0);

            if (header != null) {
                String[] parts = header.split(", ");

                for (String part : parts) {
                    ProxyAutheticationMethod method = authenticationMethod(part);

                    if (method != null) {
                        methods.add(method);
                    }
                }
            }
        }

        if (methods.contains(ProxyAutheticationMethod.PROXY_AUTH_NTLM)) {
            return ProxyAutheticationMethod.PROXY_AUTH_NTLM;
        } else if (methods.contains(ProxyAutheticationMethod.PROXY_AUTH_DIGEST)) {
            return ProxyAutheticationMethod.PROXY_AUTH_DIGEST;
        } else if (methods.contains(ProxyAutheticationMethod.PROXY_AUTH_BASIC)) {
            return ProxyAutheticationMethod.PROXY_AUTH_BASIC;
        } else {
            return null;
        }
    }


    private static ProxyAutheticationMethod authenticationMethod(String authenticateHeader) {
        String[] parts = authenticateHeader.split(" ");

        return ProxyAutheticationMethod.fromString(parts[0]);
    }

    /*
     * Split a single Proxy-Authenticate header into multiple headers, and return a specific header based on type.
     *
     * <p>
     *  Webservers or Proxy servers can choose to send proxy information as either multiple headers or one header joined
     *  by commas. Conversely the receiving side may design to combine multiple headers into a single header.
     *  As such the code below handles the former when a server sends back a single header but has multiple items in it.
     *  Examples:
     *  Proxy-Authenticate: Kerberos
     *  Proxy-Authenticate: NTLM
     *
     *  OR
     *
     *  Proxy-Authenticate: Kerberos, NTLM
     *  </p>
     *
     *  @param original Original Header we want to split up.
     *  @param type Authentication type we want
     *  @return Full authentication header for the type specified, or null if not found.
     */
    public static String parseSingleHeader(String original, ProxyAutheticationMethod type) {
        /*
         * Regular Expression Matches:
         * "word" - Just an auth type.
         * "word key=" - Auth type with key value pairs
         * "word abxcfg+/=" - Auth type with base 64 encoded data
         */
        Pattern regEx = Pattern.compile("(\\w+)\\s[\\w+\\/=]+=");

        /*
         * Take original header and seperate on ", "
         * Giving an array of:
         * [
         *      "Kerberos",
         *      "NTLM",
         *      "Digest realm=\"Hello\"",
         *      "key=\"value\"",
         *      "key2=\"value2\"",
         *      "Basic realm=\"Hello"\""
         * ]
         */
        String[] parts = original.split(", ");
        Map<String, String> headers = new HashMap<>();
        List<String> components = new ArrayList<>();
        String authType = null;

        /*
         * Iterate over all the parts and process them.
         */
        for (String part : parts) {
            Matcher ranges = regEx.matcher(part);

            /*
             * If part matches regular expression we have the start of a header.
             * There is possibly more data that needs to be tacked on.
             */
            if (ranges.find()) {
                /*
                 * The first capturing group in the regular expression is the auth type.
                 */
                String newAuthType = ranges.group(1);

                if (components.size() > 0) { // We already have a header combine it back into a single string
                    if (authType == null) {  // If this is the first authType we have seen set authType to avoid NPE
                        authType = newAuthType;
                    }

                    String header = Utils.join(", ", components);
                    headers.put(authType, header); // Store the header
                    components = new ArrayList<>(); // Reset the components

                }

                /*
                 * Start storing header components for latter.
                 */
                authType = newAuthType;
                components.add(part);
            } else if (part.contains("=")) { // This is a key value pair stash it away for latter
                components.add(part);
            } else { // Just a plan header, its authType = part
                headers.put(part, part);
            }
        }

        /*
         * Finished processing parts, combine left overs into a single header
         */
        if (components.size() > 0) {
            String lastHeader = Utils.join(", ", components);
            headers.put(authType, lastHeader);
        }

        return headers.get(type.getType()); // Return the header we want.
    }
}


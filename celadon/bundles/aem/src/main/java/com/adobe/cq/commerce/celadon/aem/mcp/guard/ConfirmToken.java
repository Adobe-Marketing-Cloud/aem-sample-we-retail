/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.celadon.aem.mcp.guard;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Stateless, self-validating confirm tokens. A token encodes
 * {@code issuedAt} + HMAC-SHA256({@code catalog|operation|argsDigest|issuedAt}).
 * No server-side store; the HMAC key is random per instance, so a restart
 * invalidates outstanding tokens (acceptable given the short TTL).
 */
public final class ConfirmToken {

    private final byte[] key = new byte[32];
    private final long ttlMillis;

    public ConfirmToken(long ttlMillis) {
        new SecureRandom().nextBytes(key);
        this.ttlMillis = ttlMillis;
    }

    public String issue(String catalog, String operation, String argsJson) {
        long issuedAt = System.currentTimeMillis();
        String payload = issuedAt + ":" + sign(catalog, operation, argsJson, issuedAt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verify(String token, String catalog, String operation, String argsJson) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int colon = payload.indexOf(':');
            if (colon < 0) {
                return false;
            }
            long issuedAt = Long.parseLong(payload.substring(0, colon));
            String mac = payload.substring(colon + 1);
            if (System.currentTimeMillis() - issuedAt > ttlMillis) {
                return false;
            }
            return constantTimeEquals(mac, sign(catalog, operation, argsJson, issuedAt));
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String catalog, String operation, String argsJson, long issuedAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String data = catalog + "|" + operation + "|" + Integer.toHexString(argsJson.hashCode()) + "|" + issuedAt;
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}

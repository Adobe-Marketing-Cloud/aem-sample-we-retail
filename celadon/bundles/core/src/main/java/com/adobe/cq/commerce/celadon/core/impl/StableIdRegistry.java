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
package com.adobe.cq.commerce.celadon.core.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StableIdRegistry {
    private final Map<String, Integer> keyToId = new LinkedHashMap<>();
    private final Map<Integer, String> idToKey = new LinkedHashMap<>();

    public synchronized int toStableNumericId(String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        Integer existing = keyToId.get(key);
        if (existing != null) {
            return existing;
        }

        int attempt = 0;
        while (true) {
            byte[] digest = sha256Utf8(attempt == 0 ? key : key + "#" + attempt);
            int candidate = ((digest[0] & 0xff) << 24)
                    | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8)
                    | (digest[3] & 0xff);
            candidate = candidate & 0x7fffffff;
            if (candidate == 0) {
                candidate = 1;
            }

            String mapped = idToKey.get(candidate);
            if (mapped == null || mapped.equals(key)) {
                idToKey.put(candidate, key);
                keyToId.put(key, candidate);
                return candidate;
            }
            attempt++;
        }
    }

    public synchronized String keyForId(int id) {
        return idToKey.get(id);
    }

    private static byte[] sha256Utf8(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

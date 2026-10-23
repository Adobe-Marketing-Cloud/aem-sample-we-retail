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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ImageValueExtractor {
    private ImageValueExtractor() {
    }

    static String extractFirstImagePath(Object value) {
        List<String> paths = extractAllImagePaths(value);
        return paths.isEmpty() ? null : paths.get(0);
    }

    @SuppressWarnings("unchecked")
    static List<String> extractAllImagePaths(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                result.addAll(extractAllImagePaths(entry));
            }
            return result;
        }
        if (value instanceof Object[] array) {
            for (Object entry : array) {
                result.addAll(extractAllImagePaths(entry));
            }
            return result;
        }
        if (value instanceof String string) {
            for (String part : string.split(",")) {
                String normalized = part.trim();
                if (!normalized.isBlank()) {
                    result.add(normalized);
                }
            }
            return result;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return List.of(value.toString());
        }
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                result.addAll(extractAllImagePaths(entry));
            }
            return result;
        }
        return List.of(value.toString());
    }
}

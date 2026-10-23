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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class PathSupport {
    static final String PRODUCT_URL_SUFFIX = ".html";

    private PathSupport() {
    }

    static String normalizeFilterPathValue(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return "";
        }
        value = value.replace("+", "%2B");
        try {
            value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        value = value.replace("%2F", "/")
                .replace("%2f", "/")
                .replace("%2B", "+")
                .replace("%2b", "+")
                .replace("+", "/");
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    static String normalizeRelativePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    static List<String> segments(String path) {
        String normalized = normalizeRelativePath(path);
        if (normalized.isBlank() || "/".equals(normalized)) {
            return List.of();
        }
        return Arrays.stream(normalized.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    static String joinPath(String parent, String child) {
        String left = normalizeRelativePath(parent);
        String right = normalizeRelativePath(child);
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + "/" + right;
    }

    static String leaf(String path) {
        List<String> segments = segments(path);
        return segments.isEmpty() ? "" : segments.get(segments.size() - 1);
    }

    static String stripProductUrlSuffix(String value) {
        if (value != null && value.endsWith(PRODUCT_URL_SUFFIX)) {
            return value.substring(0, value.length() - PRODUCT_URL_SUFFIX.length());
        }
        return value;
    }

    // Ordered, de-duplicated set of every category path (each ancestor and self) that the
    // given category memberships span. Mirrors the chain buildProductCategories reports, so
    // url_rewrites and product resolution stay consistent with the categories projection.
    static List<String> categoryChainPaths(List<String> categoryPaths) {
        List<String> result = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String categoryPath : categoryPaths) {
            String current = "";
            for (String segment : segments(categoryPath)) {
                current = joinPath(current, segment);
                if (seen.add(current)) {
                    result.add(current);
                }
            }
        }
        return result;
    }

    static String normalizeSearchText(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}

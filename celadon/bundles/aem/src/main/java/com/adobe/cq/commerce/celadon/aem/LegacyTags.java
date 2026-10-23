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
package com.adobe.cq.commerce.celadon.aem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure (Sling-free, JCR-free) helpers for the classic {@code cq:tags} of a legacy product: the
 * namespaced tag ids it carries, the taxonomy nodes behind them and their translated titles.
 */
public final class LegacyTags {

    private static final Pattern LOCALIZED_TITLE = Pattern.compile("jcr:title\\.[A-Za-z]{2,3}(_[A-Za-z]{2,4})?");

    private LegacyTags() {
    }

    /**
     * The namespaced tag ids of a classic {@code cq:tags} value, in order and without duplicates.
     * Ids without a namespace are skipped, as classic catalogs always namespace their tags.
     */
    public static List<String> tagIds(Object cqTags) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (cqTags instanceof Collection<?> values) {
            for (Object value : values) {
                addTagId(ids, value);
            }
        } else if (cqTags instanceof Object[] values) {
            for (Object value : values) {
                addTagId(ids, value);
            }
        } else {
            addTagId(ids, cqTags);
        }
        return new ArrayList<>(ids);
    }

    /**
     * The tag id itself and every ancestor up to its namespace, outermost first, e.g.
     * {@code we-retail:}, {@code we-retail:activity}, {@code we-retail:activity/skiing}.
     */
    public static List<String> selfAndAncestorIds(String tagId) {
        List<String> ids = new ArrayList<>();
        int colon = tagId.indexOf(':');
        if (colon <= 0) {
            return ids;
        }
        String namespace = tagId.substring(0, colon + 1);
        ids.add(namespace);
        StringBuilder path = new StringBuilder();
        for (String segment : tagId.substring(colon + 1).split("/")) {
            if (segment.isBlank()) {
                continue;
            }
            if (path.length() > 0) {
                path.append('/');
            }
            path.append(segment);
            ids.add(namespace + path);
        }
        return ids;
    }

    /** The path of a tag below the taxonomy root, e.g. {@code we-retail/activity/skiing}. */
    public static String taxonomyPath(String tagId) {
        int colon = tagId.indexOf(':');
        String namespace = tagId.substring(0, colon);
        String path = tagId.substring(colon + 1);
        return path.isBlank() ? namespace : namespace + "/" + path;
    }

    /** The translated titles of a taxonomy node, keyed by their {@code jcr:title.<locale>} property. */
    public static Map<String, String> localizedTitles(Map<String, Object> tagNode) {
        Map<String, String> titles = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : tagNode.entrySet()) {
            if (LOCALIZED_TITLE.matcher(entry.getKey()).matches() && entry.getValue() != null) {
                String title = entry.getValue().toString();
                if (!title.isBlank()) {
                    titles.put(entry.getKey(), title);
                }
            }
        }
        return titles;
    }

    private static void addTagId(LinkedHashSet<String> ids, Object value) {
        String id = value == null ? "" : value.toString().trim();
        if (id.indexOf(':') > 0) {
            ids.add(id);
        }
    }
}

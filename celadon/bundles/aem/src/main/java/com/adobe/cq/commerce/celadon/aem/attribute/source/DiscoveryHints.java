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
package com.adobe.cq.commerce.celadon.aem.attribute.source;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public final class DiscoveryHints {

    private static final Set<String> BUILT_IN_SKIP_PREFIXES = Set.of("jcr:", "cq:", "sling:", "rep:", "oak:");
    private static final String REQUIRED_CODE = "sku";

    private final List<Pattern> allow;
    private final List<Pattern> deny;
    private final List<String> rawAllow;

    public DiscoveryHints(List<String> allowList, List<String> denyList) {
        this.rawAllow = List.copyOf(allowList);
        this.allow = compile(allowList);
        this.deny = compile(denyList);
    }

    public boolean allows(String code) {
        for (String prefix : BUILT_IN_SKIP_PREFIXES) {
            if (code.startsWith(prefix)) return false;
        }
        if (!allow.isEmpty() && allow.stream().noneMatch(p -> p.matcher(code).matches())) return false;
        if (deny.stream().anyMatch(p -> p.matcher(code).matches())) return false;
        return true;
    }

    public void validateRequired() {
        if (rawAllow.isEmpty()) return;
        if (rawAllow.stream().anyMatch(p -> matchesGlob(p, REQUIRED_CODE))) return;
        throw new IllegalArgumentException("allowList does not include required attribute '" + REQUIRED_CODE + "'");
    }

    public static DiscoveryHints read(Resource catalogRoot) {
        Resource content = catalogRoot.getChild("jcr:content");
        if (content == null) return new DiscoveryHints(List.of(), List.of());
        ValueMap vm = content.getValueMap();
        String[] allow = vm.get("celadonAllowList", new String[0]);
        String[] deny = vm.get("celadonDenyList", new String[0]);
        return new DiscoveryHints(Arrays.asList(allow), Arrays.asList(deny));
    }

    private static List<Pattern> compile(List<String> patterns) {
        return patterns.stream().map(DiscoveryHints::globToRegex).toList();
    }

    private static Pattern globToRegex(String glob) {
        StringBuilder b = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            if (c == '*') b.append(".*");
            else b.append(Pattern.quote(String.valueOf(c)));
        }
        b.append("$");
        return Pattern.compile(b.toString());
    }

    private static boolean matchesGlob(String glob, String value) {
        return globToRegex(glob).matcher(value).matches();
    }
}

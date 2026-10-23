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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AttributeScout {

    public enum JcrType { STRING, LONG, DOUBLE, BOOLEAN, DATE }

    private static final int SELECT_PROMOTION_MIN_OBS = 8;
    private static final double SELECT_PROMOTION_MAX_CARDINALITY_RATIO = 0.2;

    private final String code;
    private final Map<JcrType, Integer> typeCounts = new EnumMap<>(JcrType.class);
    private final Set<String> distinctValues = new LinkedHashSet<>();
    private int observationsOnVariant = 0;
    private int observationsOnProduct = 0;
    private boolean seenOnProduct = false;
    private boolean seenOnVariant = false;
    private boolean declaredAxis = false;

    public AttributeScout(String code) {
        this.code = code;
    }

    public void observe(JcrType type, String value, boolean onProduct, boolean onVariant) {
        typeCounts.merge(type, 1, Integer::sum);
        if (value != null) distinctValues.add(value);
        if (onProduct) { seenOnProduct = true; observationsOnProduct++; }
        if (onVariant) { seenOnVariant = true; observationsOnVariant++; }
    }

    /**
     * Marks this attribute as a configurable variant axis explicitly declared on a master
     * product (via {@code cq:productVariantAxes}). A declared axis is authoritative: it is
     * always a {@link NormalizedType#SELECT} and at least {@link AttributeScope#VARIANT}-scoped,
     * regardless of the cardinality heuristic that would otherwise demote a high-cardinality
     * value set (e.g. many distinct colours) to a plain {@code STRING}.
     */
    public void markDeclaredAxis() {
        this.declaredAxis = true;
    }

    public boolean hadConflict() {
        return typeCounts.size() > 1;
    }

    public NormalizedType inferType() {
        if (declaredAxis) return NormalizedType.SELECT;
        if (hadConflict()) return NormalizedType.STRING;
        JcrType winner = typeCounts.keySet().iterator().next();
        return switch (winner) {
            case LONG    -> NormalizedType.INT;
            case DOUBLE  -> NormalizedType.FLOAT;
            case BOOLEAN -> NormalizedType.BOOLEAN;
            case DATE    -> NormalizedType.DATE;
            case STRING  -> shouldPromoteToSelect() ? NormalizedType.SELECT : NormalizedType.STRING;
        };
    }

    private boolean shouldPromoteToSelect() {
        int totalObs = observationsOnVariant + observationsOnProduct;
        if (totalObs < SELECT_PROMOTION_MIN_OBS) return false;
        if (!seenOnVariant) return false;
        return ((double) distinctValues.size() / totalObs) <= SELECT_PROMOTION_MAX_CARDINALITY_RATIO;
    }

    public AttributeScope inferScope() {
        boolean variant = seenOnVariant || declaredAxis;
        if (seenOnProduct && variant) return AttributeScope.BOTH;
        if (variant) return AttributeScope.VARIANT;
        return AttributeScope.PRODUCT;
    }

    public Set<String> distinctValues() {
        return new HashSet<>(distinctValues);
    }

    public String code() { return code; }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.Test;

public class CategoryMembershipTest {

    // --- spec §2 worked examples (primary = man/pants/summer) ---

    @Test
    public void onlyPrimaryAssigned_noAdditional() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man/pants/summer"), "man/pants/summer");
        assertEquals(List.of(), additional);
    }

    @Test
    public void primaryPlusSeparateBranch_keepsBranch() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man/pants/summer", "sale"), "man/pants/summer");
        assertEquals(List.of("sale"), additional);
    }

    @Test
    public void primaryPlusItsAncestors_noAdditional() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man", "man/pants", "man/pants/summer"), "man/pants/summer");
        assertEquals(List.of(), additional);
    }

    @Test
    public void separateBranchChain_keepsOnlyDeepest() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man/pants/summer", "sale", "sale/clearance"), "man/pants/summer");
        assertEquals(List.of("sale/clearance"), additional);
    }

    // --- tie-break case: two equally-deep sibling branches both survive ---

    @Test
    public void twoEquallyDeepSiblings_bothKept() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man/pants/summer", "sale/winter", "sale/clearance"), "man/pants/summer");
        // primary removed; the two sale/* leaves are an antichain -> both kept, sorted
        assertEquals(List.of("sale/clearance", "sale/winter"), additional);
    }

    // --- multi-branch antichain: ancestors collapse per branch, peers survive ---

    @Test
    public void multiBranchAntichain_keepsDeepestPerBranch() {
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                Set.of("man/pants/summer",
                        "sale", "sale/clearance",
                        "outlet", "outlet/shoes", "outlet/shoes/kids"),
                "man/pants/summer");
        assertEquals(List.of("outlet/shoes/kids", "sale/clearance"), additional);
    }

    // --- ancestor-or-self predicate (used by importer + Plan 3) ---

    @Test
    public void isAncestorOrSelf_trueForSelfAndAncestors() {
        assertTrue(CategoryMembership.isAncestorOrSelf("man", "man/pants/summer"));
        assertTrue(CategoryMembership.isAncestorOrSelf("man/pants", "man/pants/summer"));
        assertTrue(CategoryMembership.isAncestorOrSelf("man/pants/summer", "man/pants/summer"));
    }

    @Test
    public void isAncestorOrSelf_falseForDescendantsAndSiblingsAndPrefixGlitch() {
        assertFalse(CategoryMembership.isAncestorOrSelf("man/pants/summer", "man/pants"));
        assertFalse(CategoryMembership.isAncestorOrSelf("sale", "man/pants"));
        // segment-boundary safety: "man/pant" is NOT an ancestor of "man/pants/summer"
        assertFalse(CategoryMembership.isAncestorOrSelf("man/pant", "man/pants/summer"));
    }
}

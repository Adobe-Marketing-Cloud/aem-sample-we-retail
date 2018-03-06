/*
 *  Copyright 2016 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
;(function(h,$){
    var PRODUCT_GRID_CLASS = ".we-product-grid-container";

    window.CQ.WeRetailIT.MenPageLoadTest = function (h, $) {
        return new h.TestCase("Load Men category page")
            .navigateTo("/content/we-retail/us/en/men.html")
            .asserts.location("/content/we-retail/us/en/men.html", true);
    };

    window.CQ.WeRetailIT.WomenPageLoadTest = function (h, $) {
        return new h.TestCase("Load Women category page")
            .navigateTo("/content/we-retail/us/en/women.html")
            .asserts.location("/content/we-retail/us/en/women.html", true);
    };

    window.CQ.WeRetailIT.EquipmentPageLoadTest = function (h, $) {
        return new h.TestCase("Load Equipment category page")
            .navigateTo("/content/we-retail/us/en/equipment.html")
            .asserts.location("/content/we-retail/us/en/equipment.html", true);
    };

    new h.TestSuite("We.Retail Tests - Product Category", {path:"/apps/weretail/tests/check_content/product-category/ProductCategorySuite.js", register: true})
        //Test Men Page
        .addTestCase(new hobs.TestCase('Test Product Category on Men')
            // Load men page
            .execTestCase(window.CQ.WeRetailIT.MenPageLoadTest(h, $))
            // Test navbar
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            // Test hero image
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            // Test featured products
            .execTestCase(window.CQ.WeRetailIT.ProductsGridTest(h, $, PRODUCT_GRID_CLASS + ":first", 6))
            .execTestCase(window.CQ.WeRetailIT.ProductTest(h, $, PRODUCT_GRID_CLASS + ":first .we-ProductsGrid-item:first"))
            // Test "All products" button
            .execTestCase(window.CQ.WeRetailIT.ButtonTest(h, $, "div.button a.btn", "All men", "content/we-retail/us/en/products/men.html"))
            // Test featured categories
            .execTestCase(window.CQ.WeRetailIT.TeasersTest(h, $, 2))
            // Test footer
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )

        //Test Women Page
        .addTestCase(new hobs.TestCase('Test Product Category on Women')
            // Load women page
            .execTestCase(window.CQ.WeRetailIT.WomenPageLoadTest(h, $))
            // Test navbar
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            // Test hero image
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            // Test featured products
            .execTestCase(window.CQ.WeRetailIT.ProductsGridTest(h, $, PRODUCT_GRID_CLASS + ":first", 6))
            .execTestCase(window.CQ.WeRetailIT.ProductTest(h, $, PRODUCT_GRID_CLASS + ":first .we-ProductsGrid-item:first"))
            // Test "All products" button
            .execTestCase(window.CQ.WeRetailIT.ButtonTest(h, $, "div.button a.btn", "All women", "content/we-retail/us/en/products/women.html"))
            // Test featured categories
            .execTestCase(window.CQ.WeRetailIT.TeasersTest(h, $, 2))
            // Test footer
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )

        //Test Equipment Page
        .addTestCase(new hobs.TestCase('Test Product Category on Equipment')
            // Load equipment page
            .execTestCase(window.CQ.WeRetailIT.EquipmentPageLoadTest(h, $))
            // Test navbar
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            // Test hero image
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            // Test featured categories
            .execTestCase(window.CQ.WeRetailIT.TeasersTest(h, $, 6))
            // Test featured products
            .execTestCase(window.CQ.WeRetailIT.ProductsGridTest(h, $, PRODUCT_GRID_CLASS + ":first", 6))
            .execTestCase(window.CQ.WeRetailIT.ProductTest(h, $, PRODUCT_GRID_CLASS + ":first .we-ProductsGrid-item:first"))
            // Test "All products" button
            .execTestCase(window.CQ.WeRetailIT.ButtonTest(h, $, "div.button a.btn", "All equipment", "content/we-retail/us/en/products/equipment.html"))
            // Test footer
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )
    ;
})(hobs, jQuery);
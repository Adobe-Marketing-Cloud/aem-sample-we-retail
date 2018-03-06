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
;(function(h) {
    window.CQ.WeRetailIT.NavbarTest = function (h, $, numberOfItems) {
        return new h.TestCase("Check navbar")
            // Check navbars are visible
            .asserts.visible(".navbar.navbar-fixed-top", true)
            .asserts.visible(".navbar.navbar-absolute-top", true)
            // Check top navbar links are visible
            .asserts.visible(".navbar.navbar-fixed-top .navbar-right li a", true)
            // Check logo on navbar is visible
            .asserts.visible(".navbar.navbar-absolute-top a.navbar-brand", true)
            // Check links on navbar are visible
            .asserts.isTrue(function() { return window.CQ.WeRetailIT.checkItemsFound(h,
                ".header .we-navigation .cmp-navigation__item-link",
                ["Experience", "Men", "Women", "Equipment", "Products"])})
            // Check search button on navbar is visible
            .asserts.visible(".navbar.navbar-absolute-top .navbar-right-outside a .we-Icon--search", true);
    }
})(hobs);

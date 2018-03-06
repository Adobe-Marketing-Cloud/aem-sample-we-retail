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
    var BREADCRUMB_CLASS = ".we-breadcrumb .cmp-breadcrumb";
    var BREADCRUMB_ITEMS = "li a";

    window.CQ.WeRetailIT.BreadcrumbTest = function (h, $, items) {
        return new h.TestCase("Check breadcrumb")
            // Check breadcrump is visible
            .asserts.visible(BREADCRUMB_CLASS, true)
            // Check breadcrumb items are visible
            .asserts.visible(BREADCRUMB_CLASS + " " + BREADCRUMB_ITEMS)
            // Check breadcrumb items
            .asserts.isTrue(function() {return window.CQ.WeRetailIT.checkItems(h, BREADCRUMB_CLASS + " " + BREADCRUMB_ITEMS, items);});
    }
})(hobs);
/*
 *  Copyright 2019 Adobe Systems Incorporated
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

    window.CQ.WeRetailIT.AboutUsLoadTest = function (h, $) {

        return new h.TestCase("Load about-us page")
            .navigateTo("/content/we-retail/language-masters/en/about-us.html")
            .asserts.location("/content/we-retail/language-masters/en/about-us.html", true);
    }

    new h.TestSuite("We.Retail Tests - AboutUs", {path:"/apps/weretail/tests/check_content/homepage/AboutUsSuite.js", register: true})
        .addTestCase(window.CQ.WeRetailIT.AboutUsLoadTest(h, $))
        .addTestCase(window.CQ.WeRetailIT.TabItemsTest(h, $, 3))
        .addTestCase(window.CQ.WeRetailIT.TabListItemsNavigationTest(h, $));
})(hobs, jQuery);

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

    window.CQ.WeRetailIT.ExperiencesPageLoadTest = function (h, $) {
        return new h.TestCase("Load experiences")
            .navigateTo("/content/we-retail/us/en/experience.html")
            .asserts.location("/content/we-retail/us/en/experience.html", true);
    }

    window.CQ.WeRetailIT.ExperiencePageLoadTest = function (h, $) {
        return new h.TestCase("Load experience")
            .navigateTo("/content/we-retail/us/en/experience/arctic-surfing-in-lofoten.html")
            .asserts.location("/content/we-retail/us/en/experience/arctic-surfing-in-lofoten.html", true);
    }

    window.CQ.WeRetailIT.ArticleContentTest = function (h, $) {
        return new h.TestCase("Check article content")
            asserts.visible(".contentfragment", true);
            ;
    }

    new h.TestSuite("We.Retail Tests - Experience", {path:"/apps/weretail/tests/experience/ExperienceSuite.js", register: true})
         // Load all experiences page
        .addTestCase(new hobs.TestCase('Load all experiences page')
            .execTestCase(window.CQ.WeRetailIT.ExperiencesPageLoadTest(h, $))
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            .execTestCase(window.CQ.WeRetailIT.ArticlesTest(h, $, 6))
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )

        // Load specific experience page
        .addTestCase(new hobs.TestCase('Load specific experience page')
            .execTestCase(window.CQ.WeRetailIT.ExperiencePageLoadTest(h, $))
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            .execTestCase(window.CQ.WeRetailIT.BreadcrumbTest(h, $, ["English", "Experience"]))
            .execTestCase(window.CQ.WeRetailIT.ArticleContentTest(h, $))
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )
    ;
})(hobs, jQuery);
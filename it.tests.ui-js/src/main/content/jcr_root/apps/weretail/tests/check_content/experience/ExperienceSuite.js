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

    var SEGMENTS_ROOT = '/conf/we-retail/settings/wcm/segments/';
    var expectedSegments = [
        'female',
        'female-over-30',
        'female-under-30',
        'male',
        'male-over-30',
        'male-under-30',
        'order-value-75-to-100',
        'order-value-over-100',
        'over-30',
        'summer',
        'summer-female',
        'summer-female-over-30',
        'summer-female-under-30',
        'summer-male',
        'summer-male-over-30',
        'summer-male-under-30',
        'under-30',
        'winter',
        'winter-female',
        'winter-female-over-30',
        'winter-female-under-30',
        'winter-male',
        'winter-male-over-30',
        'winter-male-under-30'
    ].map(function(x) { return SEGMENTS_ROOT + x; });

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

    var checkSegmentsOnPage = new h.TestCase('Check segments on a page')
        // go to a page
        .navigateTo('%url%')
        .asserts.location('%url%', true)

        // wait for segmentation store to be ready
        .execFct(function(options, done) {
            var waitForContextHub = function() {
                if (typeof window.ContextHub !== 'undefined') {
                    var ready = ContextHub.eventing.alreadyTriggered;
                    var event = ContextHub.Constants;

                    if (ready(event.EVENT_STORE_REGISTERED + ':segmentation') && ready(event.EVENT_SEGMENT_REGISTERED)) {
                        done();
                    }
                } else {
                    requestAnimationFrame(waitForContextHub);
                }
            };

            waitForContextHub();
        })

        // check if we.retail segments are present
        .execFct(function(options, done) {
            var segments = ContextHub.SegmentEngine.SegmentManager.getAllSegments();
            var allGood = true;

            // all expected segments should be present
            for (var x = 0; x < expectedSegments.length; x++) {
                var needle = expectedSegments[x];

                if (!segments[needle]) {
                    allGood = false;
                    break;
                }
            }

            // no segments from /etc/segmentation/ should be present
            if (allGood) {
                for (var item in segments) {
                    if (segments.hasOwnProperty(item)) {
                        var segment = segments[item];

                        if (segment.path.match(/^\/etc\/segmentation\//)) {
                            allGood = false;
                            break;
                        }
                    }
                }
            }

            if (allGood) {
                done();
            }
        });

    new h.TestSuite("We.Retail Tests - Experience", {path:"/apps/weretail/tests/check_content/experience/ExperienceSuite.js", register: true})
         // Load all experiences page
        .addTestCase(new hobs.TestCase('Load all experiences page')
            .execTestCase(window.CQ.WeRetailIT.ExperiencesPageLoadTest(h, $))
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )

        // Load specific experience page
        .addTestCase(new hobs.TestCase('Load specific experience page')
            .execTestCase(window.CQ.WeRetailIT.ExperiencePageLoadTest(h, $))
            .execTestCase(window.CQ.WeRetailIT.NavbarTest(h, $, 7))
            .execTestCase(window.CQ.WeRetailIT.HeroImageTest(h, $, false))
            .execTestCase(window.CQ.WeRetailIT.BreadcrumbTest(h, $, ["English", "Experience"]))
            .execTestCase(window.CQ.WeRetailIT.FooterTest(h, $))
        )

        // Check whether all We.Retail segments get loaded on We.Retail pages and no segments from /etc/segmentation/ are present
        /*
        TODO: Enable back after investigating why the context hub is not loading
        .addTestCase(new hobs.TestCase('Check segments on We.Retail pages')
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/experience.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/men.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/women.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/equipment.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/about-us.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/products.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/products/men.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/products/women.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/products/equipment.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/trending.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/catalog.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/questions.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/groups.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/forum.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/assignments.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/ideas.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/members.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/files.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/blog.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/calendar.html' } })
            .execTestCase(checkSegmentsOnPage, false, { params: { url: '/content/we-retail/us/en/community/social.html' } })
        )
        */
    ;
})(hobs, jQuery);
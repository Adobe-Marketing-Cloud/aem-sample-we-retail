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
    var SITE_FEATURE_CLASS = ".we-SiteFeature";
    window.CQ.WeRetailIT.SiteFeaturesTest = function (h, $) {
        return new h.TestCase("Check site features")
            // Check features are visible
            .asserts.visible(SITE_FEATURE_CLASS, true)
            // Check features title/text is visible
            .asserts.visible(SITE_FEATURE_CLASS + " h4", true)
            // Check there are three features blocks
            .asserts.isTrue(function() {return h.find(SITE_FEATURE_CLASS).length == 3;})
            // Check titles and subtitles not empty
            .asserts.isTrue(function() {
                var res = true;
                h.find(SITE_FEATURE_CLASS + " h4").each(function(ix, val){
                    res = res && $(val).text().trim().length > 0;
                });
                return res;
            });
    }
})(hobs);
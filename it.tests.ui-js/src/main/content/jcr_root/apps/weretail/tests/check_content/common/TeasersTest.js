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
    window.CQ.WeRetailIT.TeasersTest = function (h, $, count) {
        return new h.TestCase("Check category teasers")
            // Check category teasers are visible
            .asserts.visible(".we-CategoryTeaser", true)
            // Check category teasers buttons are visible
            .asserts.visible(".we-CategoryTeaser .btn", true)
            // Check category teasers number
            .asserts.isTrue(function() {return window.CQ.WeRetailIT.checkNumberOfItems(h, ".we-CategoryTeaser", count);});
    }
})(hobs);

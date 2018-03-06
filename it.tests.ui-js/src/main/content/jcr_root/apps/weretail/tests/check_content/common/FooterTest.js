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
    window.CQ.WeRetailIT.FooterTest = function (h, $) {
        return new h.TestCase("Check page footer")
            // Check footer visible
            .asserts.visible("footer.we-Footer", true)
            // Check footer logo visible
            .asserts.visible("footer.we-Footer .we-Logo")
            // Check footer links visible
            .asserts.visible("footer.we-Footer .we-navigation")
            // Check footer copyright visible
            .asserts.visible("footer.we-Footer .text-uppercase")
            // Check footer #top button visible
            .asserts.visible("footer.we-Footer .btn-action-up");
    }
})(hobs);

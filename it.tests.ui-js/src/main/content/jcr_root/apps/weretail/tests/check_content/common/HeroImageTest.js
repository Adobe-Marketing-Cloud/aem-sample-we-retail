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
    window.CQ.WeRetailIT.HeroImageTest = function (h, $, buttonVisible) {
        return new h.TestCase("Check hero image")
            // Check hero image is visible
            .asserts.visible(".heroimage .jumbotron", true)
            // Check hero title is visible
            .asserts.visible(".heroimage .h1", true)
            // Check hero image button link is visible
            .asserts.visible(".heroimage a.btn", buttonVisible)
            ;
    }
})(hobs);
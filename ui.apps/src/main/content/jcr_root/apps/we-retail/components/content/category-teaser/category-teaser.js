'use strict';

var global = this;
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

use(function () {
    var linkTo      = properties.get("buttonLinkTo", "");
    var buttonLabel = properties.get("buttonLabel", "");

    if(linkTo != "") {
        // if button label is not set, try to get it from target page's title
        if(buttonLabel == "") {
            var linkResource = request.getResourceResolver().getResource(linkTo);
            if(linkResource != null) {
                var targetPage = linkResource.adaptTo(Packages.com.day.cq.wcm.api.Page);
                if (targetPage) {
                    buttonLabel = targetPage.getTitle();
                }
            }
        }

        linkTo = linkTo + ".html";
    }
    return {
        buttonLinkTo: linkTo,
        buttonLabel: buttonLabel
    };
});

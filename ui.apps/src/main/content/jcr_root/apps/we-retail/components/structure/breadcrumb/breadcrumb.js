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
"use strict";

use(function() {
    // get starting point of trail
    var level = properties.get("absParent") || currentStyle.get("absParent") || 2;
    var endLevel = properties.get("relParent") || currentStyle.get("relParent") || 0;
    var delimStr = properties.get("delim") || currentStyle.get("delim") || "/";
    var trailStr = properties.get("trail") || currentStyle.get("trail") || "";
    var currentLevel = currentPage.getDepth();

    var ret = {
        delim: delimStr,
        trail: trailStr,
        crumbs: []
    };

    while (level < currentLevel - endLevel) {
        var trail = currentPage.getAbsoluteParent(level);
        if (trail == null) {
            break;
        }

        var title = trail.getNavigationTitle();
        if (title == null || title.equals("")) {
            title = trail.getNavigationTitle();
        }
        if (title == null || title.equals("")) {
            title = trail.getTitle();
        }
        if (title == null || title.equals("")) {
            title = trail.getName();
        }
        ret.crumbs.push({
            title: title,
            trail: trail
        });

        level++;
    }

    return ret;
});
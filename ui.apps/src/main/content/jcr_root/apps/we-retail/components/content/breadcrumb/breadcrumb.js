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
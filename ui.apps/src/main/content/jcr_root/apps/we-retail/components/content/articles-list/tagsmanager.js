"use strict";

var global = this;

/**
 *
 */
use(["/libs/wcm/foundation/components/utils/AuthoringUtils.js",
     "/libs/wcm/foundation/components/utils/ResourceUtils.js",
     "/libs/sightly/js/3rd-party/q.js"], function (AuthoringUtils, ResourceUtils, Q) {

    var TagsManager = function(resource) {
        var resourceResolver = resource.getResourceResolver();
        this.tagManager = resourceResolver.adaptTo(global.Packages.com.day.cq.tagging.TagManager);
    };

    TagsManager.prototype.resolve = function(tagId) {
        console.error("tagid: " + tagId);
        return this.tagManager.resolve(tagId);
    };

    TagsManager.prototype.resolveAll = function(tagsList) {
        var ret = [];
        for (var i=0, len=tagsList.length; i<len; i++) {
            ret.push(this.resolve(tagsList[i]));
        }
        return ret;
    };



    return TagsManager;
});
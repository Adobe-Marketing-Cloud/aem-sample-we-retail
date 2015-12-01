"use strict";

var global = this;

/**
 *
 */
use(function () {

    var TagsManager = function(resource) {
        var resourceResolver = resource.getResourceResolver();
        this.tagManager = resourceResolver.adaptTo(global.Packages.com.day.cq.tagging.TagManager);
    };

    TagsManager.prototype.resolve = function(tagId) {
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
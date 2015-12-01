"use strict";

var global = this;

/**
 *
 */
use(["/libs/wcm/foundation/components/utils/AuthoringUtils.js",
     "/libs/wcm/foundation/components/utils/ResourceUtils.js",
     "/libs/sightly/js/3rd-party/q.js",
     "../tagsmanager.js",
     "../profilemanager.js"], function (AuthoringUtils, ResourceUtils, Q, TagsManager, ProfileManager) {

    var def = Q.defer(),
        ret = {
            properties: {}
        },
        tagsManager = new TagsManager(resource);



    ResourceUtils.getPageProperties(granite.resource).then(function(pageProperties) {
        var defs = [];
        ret.properties = pageProperties;
        ret.properties.tags = tagsManager.resolveAll(pageProperties['cq:tags']);

        defs.push(ProfileManager.getProfile(pageProperties['articleAuthor']).then(function(authorData) {
            ret.properties.author = authorData;
        }));

        Q.all(defs).then(function() {
            def.resolve();
        });

    });

    return def.promise.then(function() {
        return ret;
    });
});
"use strict";

var global = this;

/**
 *
 */
use(["/libs/wcm/foundation/components/utils/AuthoringUtils.js",
     "/libs/wcm/foundation/components/utils/ResourceUtils.js",
     "/libs/sightly/js/3rd-party/q.js",
     "../tagsmanager.js"], function (AuthoringUtils, ResourceUtils, Q, TagsManager) {

    var def = Q.defer(),
        ret = {
            properties: {}
        },
        tagsManager = new TagsManager(resource);



    ResourceUtils.getPageProperties(granite.resource).then(function(pageProperties) {
        ret.properties = pageProperties;
        ret.properties.tags = tagsManager.resolveAll(pageProperties['cq:tags']);
        def.resolve();
    });

    return def.promise.then(function() {
        return ret;
    });
});
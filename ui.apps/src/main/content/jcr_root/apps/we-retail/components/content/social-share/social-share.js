'use strict';

var global = this;

use(['/libs/wcm/foundation/components/utils/ResourceUtils.js'], function (ResourceUtils) {
    var CONST = {
        FACEBOOK: 'facebookShare',
        TWITTER: 'twitterShare',
        GOOGLE_PLUS: 'googlePlusShare',
        PINTEREST: 'pinterestShare'
    };

    // TODO: get settings from global configuration?
    var enabledPlugins = {
        facebook: true, //global.granite.resource.properties[CONST.FACEBOOK],
        twitter: true, //global.granite.resource.properties[CONST.TWITTER],
        googlePlus: true, // global.granite.resource.properties[CONST.GOOGLE_PLUS],
        pinterest: true //global.granite.resource.properties[CONST.PINTEREST]
    };

    return {
        enabledPlugins: enabledPlugins
    }
});
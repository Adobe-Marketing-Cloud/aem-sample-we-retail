'use strict';

use(['/libs/wcm/foundation/components/image/v2/image/image.js'], function (image) {
    var CONST = {
        FULL_WIDTH: 'useFullWidth',
        KEEP_RATIO: 'keepRatio'
    };

    var classList = 'we-HeroImage';

    if (granite.resource.properties[CONST.FULL_WIDTH] == 'true') {
        classList += ' width-full';
    }
    if (granite.resource.properties[CONST.KEEP_RATIO] == 'true') {
        classList += ' ratio-16by9';
    }

    return {
        classList: classList,
        image: image
    };
});
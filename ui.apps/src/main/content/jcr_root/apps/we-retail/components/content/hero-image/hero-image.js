'use strict';
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

var global = this;

use(function () {
    var CONST = {
        FULL_WIDTH: 'useFullWidth',
        KEEP_RATIO: 'keepRatio'
    };

    var classList = 'we-HeroImage';

    if (global.granite.resource.properties[CONST.FULL_WIDTH] == 'true') {
        classList += ' width-full';
    }
    if (global.granite.resource.properties[CONST.KEEP_RATIO] == 'true') {
        classList += ' ratio-16by9';
    }

    var renditionPath = resource.path + ".img.jpeg";
    return {
        classList: classList,
        image: {
            src: renditionPath
        }
    };
});
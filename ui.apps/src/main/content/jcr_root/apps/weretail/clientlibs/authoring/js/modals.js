/***************************************************************************
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
 **************************************************************************/
(function ($) {
    'use strict';

    // make sure modals are displayed in the middle of the page when in preview mode
    $('.modal').on('show.bs.modal', function () {
        $('.modal-dialog.modal-center', this).each(function () {
            var viewportHeight = parent.innerHeight;
            $(this).css('top', viewportHeight * 0.5);
        });
    });

}(jQuery));

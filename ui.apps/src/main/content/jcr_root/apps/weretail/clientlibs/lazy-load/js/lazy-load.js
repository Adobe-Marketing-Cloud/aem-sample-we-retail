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
(function (window, $) {
    'use strict';

    function loadImages() {
        $("img[data-lazy-src]").not("img[data-lazy-src][src]").each(function () {
            var $img = $(this);
            $img.attr('src', $img.data('lazySrc'));
        });
    }

    $(window).load(loadImages);

    if (typeof ContextHub !== "undefined" && ContextHub != null) {
        ContextHub.eventing.on(ContextHub.Constants.EVENT_TEASER_LOADED, loadImages);
    }

    var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;
    var body = document.querySelector('body');
    var observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
            // needed for IE
            var nodesArray = [].slice.call(mutation.addedNodes);
            if (nodesArray.length > 0) {
                nodesArray.forEach(function (addedNode) {
                    if(addedNode.querySelectorAll) {
                        var lazyImages = $(addedNode).find('img[data-lazy-src]').not("img[data-lazy-src][src]");
                        if(lazyImages.length) {
                            loadImages();
                        }
                    }
                });
            }
        });
    });

    observer.observe(body, {
        subtree: true,
        childList: true,
        characterData: true
    });

})(window, jQuery);

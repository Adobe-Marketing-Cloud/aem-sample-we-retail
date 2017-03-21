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
(function ($, document) {

    function init() {
        var $navbarShutter = $('.navbar-absolute-top .navbar-shutter');

        $('.navbar-absolute-top')
            .on('mouseenter', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
                e.target.classList.add('hover');
                e.delegateTarget.classList.add('submenu-opened');
                $navbarShutter.height($(e.target).next('.navbar-nav-subitems').innerHeight());
            })
            .on('mouseleave', '.navbar-center li:has(.navbar-nav-subitems)', function (e) {
                (e.target.tagName === 'UL' ? e.target.previousElementSibling : e.target).classList.remove('hover');
                e.delegateTarget.classList.remove('submenu-opened');
                $navbarShutter.height(0);
            });
    }

    init();

    $(document).bind('we-header-loaded', function() {
        init();
    });
})(jQuery, document);
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
    var $headerContainer = $('.we-retail-header'),
        url = $headerContainer.data('we-header-content');

    if (url !== undefined && url !== "") {
        $.ajax({
            type   : "GET",
            url    : "/libs/granite/security/currentuser.json",
            async  : true,
            success: function (json) {
                // toggle visibility of header elements as per the current logged in user
                loadHeader(json['authorizableId'], url);

                // On publish: load the request user into ContextHub
                if (typeof ContextHub !== "undefined") {
                    var profileStore   = ContextHub.getStore('profile');
                    var requestUser    = json["home"];
                    var contextHubUser = profileStore.getTree().path;
                    if (!contextHubUser || contextHubUser !== requestUser) {
                        profileStore.loadProfile(requestUser);
                    }
                }
            }
        });
    }

    /**
     * JS include of header component, bypass dispatcher caching if user is logged in
     * @param currentUser The id of the current logged in user
     * @param url to load the header for inclusion
     * (should be anonymous if there is no logged in user)
     */
    function loadHeader(currentUser, url) {
        $.ajax({
            type   : "GET",
            url    : url,
            cache  : currentUser === "anonymous",
            success: function (data) {
                var $navbar = $(data).find('.navbar');
                if($navbar.length) {
                    $headerContainer.prepend(data);
                    $(document).trigger('we-header-loaded');
                }
            }
        });
    }

    function unreadCounter($el, path, successHandler) {
        if($el.length) {
            var siteUrl = $el.data('siteurl'),
                data = {};

            $.ajax({
                type: "GET",
                url: CQ.shared.HTTP.getContextPath() + siteUrl + path,
                async: true,
                cache: false,
                data: data,
                success: function(json) {
                    $el.text(successHandler(json));
                }
            });
        }
    }

    $(document).on('we-header-loaded', function() {
        unreadCounter($('#we-retail-message-count'),
            "/messaging/jcr:content/content/primary/messagebox_5ab3.social.0.0.json", function(json) {
                return json["messageCounts"].nonDeletedUnreadCount;
            });

        unreadCounter($('#we-retail-notification-count'),
            "/notifications/jcr:content/content/primary/notifications.social.0.0.json", function(json) {
                return json.unreadCount;
            });

    });
})(jQuery, document);
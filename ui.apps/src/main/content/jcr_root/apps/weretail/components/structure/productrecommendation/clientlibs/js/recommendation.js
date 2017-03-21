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
(function() {
    function doUpdate() {
        $(".recommendations-viewer").each(function() {
            var $viewer = $(this),
                relationshipType = $viewer.attr("data-relationship-type"),
                maxCount = $viewer.attr("data-max-count"),
                $template = $viewer.find("script[type='text/x-handlebars-template']"),
                store = null;

            $viewer.find(".recommendations-content").empty();

            if (relationshipType) {
                relationshipType = relationshipType.split(":");
            }
            if (relationshipType && relationshipType.length >= 2) {
                store = window.ContextHub ? ContextHub.getStore(relationshipType[0]) : null;
            }

            if (!$template || !store) {
                $viewer.find(".recommendations-default").show();
                return;
            }

            var relationships = store.products(maxCount, relationshipType[1]),
                template = Handlebars.compile($template.html());

            $viewer.find(".recommendations-default").toggle(relationships.length == 0);
            $viewer.find(".recommendations-content").toggle(relationships.length > 0);

            var html = [];
            for (var i = 0; i < relationships.length; i++) {
                html.push(template(relationships[i]));
            }
            $viewer.find(".recommendations-content").html(html.join(""));
        });
    }

    doUpdate();

    if (window.ContextHub) {
        ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":relatedproducts", doUpdate);
    }
}).call(this);
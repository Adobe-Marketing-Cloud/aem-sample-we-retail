(function() {
    function doUpdate() {
        $(".recommendations-viewer").each(function() {
            var $viewer = $(this),
                relationshipType = $viewer.attr("data-relationship-type"),
                maxCount = $viewer.attr("data-max-count"),
                $template = $viewer.find("script[type='text/x-handlebars-template']"),
                store = null;

            $viewer.find(".product-recommendation").remove();

            if (relationshipType) {
                relationshipType = relationshipType.split(":");
            }
            if (relationshipType && relationshipType.length >= 2) {
                store = ContextHub ? ContextHub.getStore(relationshipType[0]) : null;
            }

            if (!$template || !store) {
                $viewer.find(".recommendations-default").show();
                return;
            }

            var relationships = store.products(maxCount, relationshipType[1]),
                template = Handlebars.compile($template.html());

            $viewer.find(".recommendations-default").toggle(relationships.length == 0);

            var html = [];
            for (var i = 0; i < relationships.length; i++) {
                html.push(template(relationships[i]));
            }
            $viewer.html(html.join(""));
        });
    }

    doUpdate();

    if (window.ContextHub) {
        ContextHub.eventing.on(ContextHub.Constants.EVENT_STORE_UPDATED + ":relatedproducts", doUpdate);
    }
}).call(this);
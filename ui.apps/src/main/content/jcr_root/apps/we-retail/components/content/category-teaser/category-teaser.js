'use strict';

var global = this;

use(function () {
    var linkTo 		= properties.get("buttonLinkTo", "");
    var buttonLabel = properties.get("buttonLabel", "");
    
    if(linkTo == "") {
        linkTo = "#";
    } else {
    	// if button label is not set, try to get it from target page's title
    	if(buttonLabel == "") {
    		var linkResource = request.getResourceResolver().getResource(linkTo);
    		
    		if(linkResource != null) {
    			var targetPage = linkResource.adaptTo(Packages.com.day.cq.wcm.api.Page); 
    			buttonLabel = targetPage.getTitle();
    		}
        }
    	
        linkTo = linkTo + ".html";
    }
    
    return {
        buttonLinkTo: linkTo,
        buttonLabel: buttonLabel
    };
});

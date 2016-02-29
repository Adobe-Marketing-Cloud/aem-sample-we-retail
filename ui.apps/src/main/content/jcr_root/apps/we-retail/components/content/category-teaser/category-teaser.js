'use strict';

var global = this;

use(function () {
    var linkTo = properties.get("buttonLinkTo", "");

    if(linkTo == "") {
    	linkTo = "#";
    } else {
    	linkTo = linkTo + ".html";
    }
    
    return {
    	buttonLinkTo: linkTo
    };
});

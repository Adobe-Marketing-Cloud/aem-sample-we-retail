	$.getJSON( "/libs/granite/security/currentuser.json", function( data ) {
		if (data.authorizableId=="anonymous") {
            $CQ(".we-retail-anonymous").show();
            $CQ(".we-retail-not-anonymous").hide();
		} else {
	        $CQ("#header-avatar").attr("src", data.home + "/profile/photos/primary/image.prof.thumbnail.48.png").attr("alt",data.name).attr("title",data.name);
	        $CQ(".we-retail-anonymous").hide();
            $CQ(".we-retail-not-anonymous").show();
		}
	});
function getSections() {
    var path = location.pathname;
    path = path.substring(0, path.length - 5);

	$.get("http://localhost:4502/bin/querybuilder.json?type=cq:Page&path="+path+"&p.nodedepth=1&property=@jcr:content/cq:commerceType&property.value=section&p.limit=-1&orderby=@jcr:content/cq:lastModified", function(data) {
        if(data['results']) $(".sections").append('<h2>Sections</h2>');
        for(var s in data['hits']) {
            console.log(s);
            $(".sections").append('<a href="'+data['hits'][s].path+'.html">'+data['hits'][s].title+'</a>&nbsp&nbsp');
        }
    });
}

function generateTagFilter(tag) {
    var reqTags = [];
	$("input#tag").each(function() {
        reqTags.push($(this).val());
    });
    var cond = true;
    for(var t in reqTags) {
		if(reqTags[t] == tag) cond = false;
    }
    if(cond) {
        $(".filter.tags").append('<div><input type="checkbox" name="tag" id="tag" value="'+tag+'" checked>'+tag.substring(1+tag.lastIndexOf('/'))+'</div>');
    }
}

function displayProduct(data, path, filters) {
    var name = data['jcr:title'];
    var description = data['jcr:description'];
    var price = data['price'];
    var img = data['image']['fileReference'];
    var condition = filters.first;
	for(var t in data['cq:tags']) {
        generateTagFilter(data['cq:tags'][t]);
        for(var tag in filters.tags) {
        	if( data['cq:tags'][t] == filters.tags[tag]) condition = true;
        }
    }
    if(price < filters.minPrice || price > filters.maxPrice) condition = false;
	if(condition) var res = '<div class="result-wrapper"><div class="result"><div class="img-wrapper"><img src="'+img+'" /></div><b><a href=\"'+path+'.html\">'+name+'</a></b><p>'+description+'</p><p>$'+price+'</p></div></div>';
    $(".results").append(res);
}

function displayPage(data, filters) {
    var productPath = data['jcr:content']['cq:productMaster'];
    $.get(productPath+'.1.json', function (d) {displayProduct(d, productPath, filters);});
}

function displayResults(data, filters) {
	for(var hit in data.hits) {
		$.get(data.hits[hit].path+'.1.json', function(d) {displayPage(d, filters);});
    }
}

function search(first) {
    var path = location.pathname;
    path = path.substring(0, path.length - 5);

    $(".results").html("<h2>Products</h2>");
    var filters = {};
    var reqTags = [];
   	$("input#tag").each(function() {
       	if($(this).prop("checked")) reqTags.push($(this).val());
    });
    minPrice = $("#minPrice").val();
    if(! minPrice) minPrice = 0;
    maxPrice = $("#maxPrice").val();
	if(! maxPrice) maxPrice = 10000;
    filters.minPrice = minPrice;
    filters.maxPrice = maxPrice;
    filters.tags = reqTags;
    filters.first = first;
    $.get("http://localhost:4502/bin/querybuilder.json?type=cq:Page&path="+path+"&property=@jcr:content/cq:template&property.value=/apps/we-retail/templates/page-product&p.limit=-1&orderby=@jcr:content/cq:lastModified", function(data) {displayResults(data, filters);});
}

$(window).ready(function() {
    getSections();
    $(".filters input").change(function() {
    	var reqTags = [];
		$("input#tag").each(function() {
        	if($(this).prop("checked")) reqTags.push($(this).val());
        });
        minPrice = $("#minPrice").val();
        if(! minPrice) minPrice = 0;
        maxPrice = $("#maxPrice").val();
		if(! maxPrice) maxPrice = 10000;
    });

	$("#search").click(function() {
    	search(0);
    });
    search(1);
});

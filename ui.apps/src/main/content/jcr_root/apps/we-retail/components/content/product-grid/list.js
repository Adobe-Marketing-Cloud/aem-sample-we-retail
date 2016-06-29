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
"use strict";

var global = this;

/**
 * List foundation component JS backing script
 */
use(["/libs/wcm/foundation/components/utils/AuthoringUtils.js",
    "/libs/wcm/foundation/components/utils/ResourceUtils.js",
    "/libs/sightly/js/3rd-party/q.js"], function (AuthoringUtils, ResourceUtils, Q) {

    var CONST = {
        PROP_SOURCE: "listFrom",
        PROP_QUERY: "query",
        PROP_SEARCH_IN: "searchIn",
        PROP_TYPE: "displayAs",
        PROP_ORDER_BY: "orderBy",
        PROP_LIMIT: "limit",
        PROP_PAGE_MAX: "pageMax",
        PROP_ORDERED: "ordered",
        PROP_PARENT_PAGE: "parentPage",
        PROP_FEED_ENABLED: "feedEnabled",
        PROP_SAVED_QUERY: "savedquery",
        PROP_TAG_SEARCH_ROOT: "tagsSearchRoot",
        PROP_TAGS: "tags",
        PROP_TAGS_MATCH: "tagsMatch",
        PROP_PAGES: "pages",

        PARAM_PAGE_START: "start",
        PARAM_PAGE_MAX: "max",

        SOURCE_CHILDREN: "children",
        SOURCE_DESCENDANTS: "descendants",
        SOURCE_STATIC: "static",
        SOURCE_SEARCH: "search",
        SOURCE_QUERYBUILDER: "querybuilder",
        SOURCE_TAGS: "tags",

        TYPE_DEFAULT: "default",

        PAGE_MAX_DEFAULT: -1,
        LIMIT_DEFAULT: 100
    };

    var _getModifiedDate = function (pageResource) {
        var modifDatePromise = Q.defer();

        var parseDate = function (dateProperty, promise) {
            try {
                var month = dateProperty.get(global.Packages.java.util.Calendar.MONTH) + 1;
                var day = dateProperty.get(global.Packages.java.util.Calendar.DAY_OF_MONTH);
                var year = dateProperty.get(global.Packages.java.util.Calendar.YEAR);
                var hourOfDay = dateProperty.get(global.Packages.java.util.Calendar.HOUR_OF_DAY);
                var minutes = dateProperty.get(global.Packages.java.util.Calendar.MINUTE);

                var modifDate = month + "/" + day + "/" + year + " " + hourOfDay + ":" + minutes;

                log.debug("Page " + pageResource.path + " modification date is " + modifDate);

                promise.resolve(modifDate);
            } catch (e) {
                log.error("Can't determine page " + pageResource.path + " modification date" + e);

                promise.resolve(undefined);
            }
        };

        var propValue = pageResource.properties["date"];
        if (!propValue) {
            ResourceUtils.getResource(pageResource.path + "/jcr:content")
                .then(function (pageContentResource) {
                    propValue = pageContentResource.properties["cq:lastModified"]
                        || pageContentResource.properties["jcr:lastModified"];

                    parseDate(propValue, modifDatePromise);
                });
        } else {
            parseDate(propValue, modifDatePromise);
        }

        return modifDatePromise.promise;
    }

    var _hasImage = function (pageResource) {
        var hasImagePromise = Q.defer();

        ResourceUtils.getResource(pageResource.path + "/jcr:content")
            .then(function (pageContentResource) {

                if (pageContentResource.properties["fileReference"]) {
                    hasImagePromise.resolve(true);
                } else {
                    ResourceUtils.getResource(pageContentResource.path + "/image")
                        .then(function (imageChildResource) {
                            if (imageChildResource.properties["fileReference"]) {
                                hasImagePromise.resolve(true);
                            } else {
                                ResourceUtils.getResource(imageChildResource.path + "/file")
                                    .then(function (imageFileResource) {
                                        hasImagePromise.resolve(true);
                                    }, function () {
                                        hasImagePromise.resolve(false);
                                    });
                            }
                        }, function () {
                            hasImagePromise.resolve(false);
                        });
                }

            }, function () {
                hasImagePromise.resolve(false);
            });

        return hasImagePromise.promise;
    }

    var _getAbsoluteParent = function (handle, level) {
        var idx = 0;
        var len = handle.length;
        while (level >= 0 && idx<len) {
            idx = handle.indexOf('/', idx+1);
            if (idx < 0) {
                idx=len;
            }
            level--;
        }
        return level>=0 ? "" : handle.substring(0,idx);
    };

    var _initializeProperties = function () {
        var currentListId = _getGeneratedId();
        log.info("List generated ID: " + currentListId);

        var source = granite.resource.properties[CONST.PROP_SOURCE]
            || CONST.SOURCE_STATIC;

        var query = granite.resource.properties[CONST.PROP_QUERY]
            || "";

        var startIn = granite.resource.properties[CONST.PROP_SEARCH_IN]
            || _getAbsoluteParent(granite.resource.path, 1);

        var type = granite.resource.properties[CONST.PROP_TYPE]
            || CONST.TYPE_DEFAULT;

        var orderBy = granite.resource.properties[CONST.PROP_ORDER_BY]
            || "";

        var limit = granite.resource.properties[CONST.PROP_LIMIT]
            || CONST.LIMIT_DEFAULT;
        limit = parseInt(limit);
        if (limit <= 0) {
            limit = CONST.LIMIT_DEFAULT;
        }

        var pageMax = granite.resource.properties[CONST.PROP_PAGE_MAX]
            || CONST.PAGE_MAX_DEFAULT;
        pageMax = parseInt(pageMax);
        if (pageMax <= 0) {
            pageMax = CONST.PAGE_MAX_DEFAULT;
        }

        var ordered = granite.resource.properties[CONST.PROP_ORDERED]
            || "";

        var feedEnabled = granite.resource.properties[CONST.PROP_FEED_ENABLED]
        if (!feedEnabled) {
            feedEnabled = false;
        }

        var pageStart = 0;
        if (granite.request.parameters[_getListSpecificParamName(CONST.PARAM_PAGE_START)]) {
            pageStart = parseInt(granite.request.parameters[_getListSpecificParamName(CONST.PARAM_PAGE_START)]);
        }

        if (granite.request.parameters[_getListSpecificParamName(CONST.PARAM_PAGE_MAX)]) {
            pageMax = granite.request.parameters[_getListSpecificParamName(CONST.PARAM_PAGE_MAX)];
        }

        var listCfg = {
            source: source,
            query: query,
            startIn: startIn,
            type: type,
            orderBy: orderBy,
            limit: limit,
            ordered: ordered,
            pageStart: pageStart,
            pageMax: pageMax,
            feedEnabled: feedEnabled
        };

        for (var cfgItem in listCfg) {
            log.debug("List config - " + cfgItem + " = " + listCfg[cfgItem]);
        }

        return listCfg;
    };

    var _getGeneratedId = function() {
        var resPath = granite.resource.path;
        var pgRoot = "jcr:content/";

        var root = resPath.indexOf(pgRoot);
        if (root >= 0) {
            resPath = resPath.substring(root + pgRoot.length);
        }

        return resPath.replace('/', '_');
    };

    var _getListSpecificParamName = function(paramName) {
        var listId = _getGeneratedId();
        return listId + "_" + paramName;
    };

    var _addItems = function (resultPromise, itemList, listConfig) {
        if (!itemList) {
            return;
        }

        var destination = [];

        var collectItems = function (allItems, currentIndex, collector, collectPromise) {
            if (!collectPromise) {
                collectPromise = Q.defer();
                log.debug("Collecting all " + allItems.length + " result items");
            }

            if (!collector) {
                collector = [];
            }

            if (allItems.length == 0) {
                log.debug("Resolving collection, total " + collector.length + " items");
                collectPromise.resolve(collector);
                return collectPromise.promise;
            }

            var item = allItems[currentIndex];
            log.debug("Collecting item " + item.path);
            ResourceUtils.getContainingPage(item)
                .then(function (pageContainingItem) {
                    log.debug("Collecting item's page " + pageContainingItem.path);
                    // need to set item.title to jcr:title of jcr:content node
                    ResourceUtils.getPageProperties(pageContainingItem)
                        .then(function (pageProps) {
                            pageContainingItem.title = pageProps["jcr:title"];
                            pageContainingItem.orderByData = pageProps[listConfig.orderBy];

                            collector.push(pageContainingItem);

                            if (currentIndex + 1 <= allItems.length -1) {
                                log.debug("Collecting next item in the list");
                                collectItems(allItems, currentIndex + 1, collector, collectPromise);
                            } else {
                                log.debug("Resolving collection, total " + collector.length + " items");
                                collectPromise.resolve(collector);
                            }
                        });
                });

            return collectPromise.promise;
        };

        collectItems(itemList, 0)
            .then(function (fullList) {
                log.debug("Collected " + fullList.length + " total items for the list");
                // apply ordering, if any
                if (listConfig.orderBy) {
                    log.debug("Need to order the list by [" + listConfig.orderBy + "]");
                    fullList.sort(function (page1, page2) {
                        var prop1 = page1.orderByData
                            || page2.orderByData
                            || "";
                        var prop2 = page2.orderByData
                            || page1.orderByData
                            || "";
                        log.debug("[Sorting list] Comparing " + prop1 + " with " + prop2);
                        if (prop1.compareTo) {
                            log.debug("[Sorting list] Using 'compareTo' method for comparison");
                            return prop1.compareTo(prop2)
                        } else if (prop1.localeCompare) {
                            log.debug("[Sorting list] Using 'localeCompare' method for comparison");
                            return prop1.localeCompare(prop2);
                        }
                        return -1;
                    });
                    log.debug("Sorting finished!");
                }

                var currentIndex = -1;
                var count = 0;
                var hasMore = false;
                for (var currentIndex = 0 ; currentIndex < fullList.length ; currentIndex++) {
                    // skip items until start index or stop if global limit is reached
                    if (currentIndex >= listConfig.limit) {
                        break;
                    }
                    if (listConfig.pageStart >= 0 && currentIndex < listConfig.pageStart) {
                        continue;
                    }

                    // stop when reached page maximum
                    if (listConfig.pageMax > 0 && count >= listConfig.pageMax ) {
                        hasMore = true;
                        break;
                    }

                    count++;
                    destination.push({
                        item: fullList[currentIndex],
                        itemName: fullList[currentIndex].title,
                        modifdate: _getModifiedDate(fullList[currentIndex]),
                        hasimage: _hasImage(fullList[currentIndex])
                    });
                }

                // apply limit
                destination = destination.slice(0, listConfig.limit)

                resultPromise.resolve({
                    renderList: destination,
                    hasMore: hasMore
                });
            });
    }

    var _getRequestQueryString = function(replaceParam, replaceParamValue) {
        var queryString = "";

        var found = false;
        for (var paramName in granite.request.parameters) {
            var paramValue = granite.request.parameters[paramName];
            if (paramName == replaceParam) {
                paramValue = replaceParamValue;
                found = true;
            }
            queryString += paramName + "=" + paramValue + "&";
        }

        if (!found) {
            queryString += replaceParam + "=" + replaceParamValue;
        } else if (queryString.length > 0) {
            queryString = queryString.substr(0, queryString.length - 1);
        }

        return queryString;
    };

    var _getPrevPage = function (listConfig) {
        var prevPageLinkPromise = Q.defer();
        if (listConfig.pageStart > 0) {
            var startPoint = listConfig.pageMax > 0 && listConfig.pageStart > listConfig.pageMax ?
            listConfig.pageStart - listConfig.pageMax : 0;

            var queryString = _getRequestQueryString(_getListSpecificParamName(CONST.PARAM_PAGE_START), startPoint);
            var prevPage = granite.resource.path + ".html?" + queryString;
            log.debug("Computed previous page link: " + prevPage);
            prevPageLinkPromise.resolve(prevPage);
        } else {
            prevPageLinkPromise.resolve("");
        }

        return prevPageLinkPromise.promise;
    };

    var _getNextPage = function (listConfig, hasMore) {
        var nextPageLinkPromise = Q.defer();
        if (hasMore) {
            var startPoint = listConfig.pageStart + listConfig.pageMax;

            var queryString = _getRequestQueryString(_getListSpecificParamName(CONST.PARAM_PAGE_START), startPoint);
            var nextPage = granite.resource.path + ".html?" + queryString;
            nextPageLinkPromise.resolve(nextPage);
        } else {
            nextPageLinkPromise.resolve("");
        }


        return nextPageLinkPromise.promise;
    }

    var _addChildItems = function (listConfig) {
        // get parent path, default to current page
        var resultPromise = Q.defer();
        var parentPath = granite.resource.properties[CONST.PROP_PARENT_PAGE]
            || granite.resource.path;

        log.debug("Listing child items of " + parentPath);
        ResourceUtils.getResource(parentPath)
            .then(function (startResource) {
                ResourceUtils.getContainingPage(startResource).then(function (startPage) {
                    log.debug("Adding children of page " + startPage.path + " to the list component");
                    startPage.getChildren().then(function (childList) {
                        // remove the jcr:content child, if it exists
                        for(var childIdx = 0 ; childIdx < childList.length ; childIdx++) {
                            if (childList[childIdx].name == "jcr:content") {
                                childList.splice(childIdx, 1);
                                break;
                            }
                        }
                        _addItems(resultPromise, childList, listConfig, undefined);
                    });
                });
            });

        return resultPromise.promise;
    };

    var _addDescendantItems = function (listConfig) {
        var resultPromise = Q.defer();

        if (!global.Packages) {
            console.warn("Did not detect AEM platform, advanced search mode of list component not available!");
        } else {
            var resourceResolver = request.getResourceResolver();
            var session = resourceResolver.adaptTo(global.Packages.javax.jcr.Session);
            var queryBuilder = resourceResolver.adaptTo(global.Packages.com.day.cq.search.QueryBuilder);

            var params = {};
            params["path"] = properties.get("path", currentPage.getPath());
            params["type"] = "cq:Page";
            params["p.offset"] = "0";
            params["p.limit"] = "500";

            var query = queryBuilder.createQuery(global.Packages.com.day.cq.search.PredicateGroup.create(params), session);

            if (query) {
                var searchResult = query.getResult();
                if (searchResult && searchResult.getHits) {
                    var hitsIterator = searchResult.getHits().iterator();

                    _deferredCollectSearchItems(hitsIterator).then(function (itemList) {
                        _addItems(resultPromise, itemList, listConfig);
                    });
                }
            }
        }

        return resultPromise.promise;
    };

    /**
     * Collects search result items returned through an iterator
     * Returns a promise that will be resolved asynchronous
     */
    var _deferredCollectSearchItems = function (itemIterator, itemPromise, itemList) {
        if (!itemPromise) {
            itemPromise = Q.defer();
            log.debug("Deferred collecting items from an iterator");
        }

        if (!itemList) {
            itemList = [];
        }

        if (itemIterator.hasNext()) {
            var resPath = "";
            try {
                var nextItem = itemIterator.next();
                if (nextItem.getResource) {
                    resPath = nextItem.getResource().getPath();
                } else if (nextItem.getPath) {
                    resPath = nextItem.getPath();
                } else {
                    log.warn("Can't determine search item path " + nextItem);
                    resPath = nextItem;
                }
            } catch (e) {
                log.error(e);
            }
            log.debug("Deferred collecting item with path " + resPath);
            granite.resource.resolve(resPath).then(
                function (hitResource) {
                    itemList.push(hitResource);
                    _deferredCollectSearchItems(itemIterator, itemPromise, itemList);
                }
            );
        } else {
            log.debug("Iterator has no more items, resolving collection with " + itemList.length);
            itemPromise.resolve(itemList);
        }

        return itemPromise.promise;
    };

    var _addSimpleSearchItems = function (listConfig) {
        var resultPromise = Q.defer();
        if (!global.Packages) {
            console.warn("Did not detect AEM platform, simple search mode of list component not available!");
        }

        if (global.Packages) {
            var resourceResolver = resource.getResourceResolver();
            var search = resource.adaptTo(global.Packages.com.day.cq.search.SimpleSearch);
            search.setQuery(listConfig.query);
            search.setSearchIn(listConfig.startIn);
            var pagePredicate = new global.Packages.com.day.cq.search.Predicate("type", "type");
            pagePredicate.set("type", "cq:Page");
            search.addPredicate(pagePredicate);
            search.setHitsPerPage(100000);
            var searchResult = search.getResult();
            if (searchResult && searchResult.getHits) {

                var hitsIterator = searchResult.getHits().iterator();

                _deferredCollectSearchItems(hitsIterator).then(function (itemList) {
                    _addItems(resultPromise, itemList, listConfig);
                });
            }
        }

        return resultPromise.promise;
    };

    var _addQueryBuilderSearchItems = function (listConfig) {
        var resultPromise = Q.defer();
        if (!global.Packages) {
            console.warn("Did not detect AEM platform, advanced search mode of list component not available!");
        } else {
            var resourceResolver = resource.getResourceResolver();
            var session = resourceResolver.adaptTo(global.Packages.javax.jcr.Session);
            var queryBuilder = resourceResolver.adaptTo(global.Packages.com.day.cq.search.QueryBuilder);
            var query = queryBuilder.loadQuery(resource.getPath() + "/" + CONST.PROP_SAVED_QUERY, session);
            if (query) {
                query.setHitsPerPage(listConfig.limit);
                var searchResult = query.getResult();
                if (searchResult && searchResult.getHits) {
                    var hitsIterator = searchResult.getHits().iterator();

                    _deferredCollectSearchItems(hitsIterator).then(function (itemList) {
                        _addItems(resultPromise, itemList, listConfig);
                    });
                }
            }
        }

        return resultPromise.promise;
    };

    var _addTagSearchItems = function (listConfig) {
        var resultPromise = Q.defer();
        if (!global.Packages) {
            console.warn("Did not detect AEM platform, tag search mode of list component not available!");
        } else {
            var parentPath = granite.resource.properties[CONST.PROP_TAG_SEARCH_ROOT]
                || granite.resource.path;

            var tags = granite.resource.properties[CONST.PROP_TAGS]
                || [];

            var matchAny = granite.resource.properties[CONST.PROP_TAGS_MATCH] == "any";
            log.debug("Performing tag search under " + parentPath);
            ResourceUtils.getResource(parentPath).then(function (startResource) {
                log.debug("Performing tag search under page " + startResource.path);
                ResourceUtils.getContainingPage(startResource).then(function (startPage) {
                    log.debug("Searching " + tags.length + " tags under " + startPage.path);
                    if (tags.length > 0) {
                        var resourceResolver = resource.getResourceResolver();
                        var tagManager = resourceResolver.adaptTo(global.Packages.com.day.cq.tagging.TagManager);
                        var tagResultsIterator = tagManager.find(startPage.path, tags, matchAny);

                        _deferredCollectSearchItems(tagResultsIterator).then(function (itemList) {
                            _addItems(resultPromise, itemList, listConfig);
                        });
                    } else {
                        log.debug("No tags to search, return empty list!");
                        resultPromise.resolve({
                            renderList: [],
                            hasMore: false
                        });
                    }
                });
            });
        }

        return resultPromise.promise;
    };

    var _addFixedPathsItems = function (resultList, listConfig) {
        var resultPromise = Q.defer();

        var pagePaths = granite.resource.properties[CONST.PROP_PAGES]
            || [];

        if (typeof pagePaths.length == "function") {
            // got only one string, convert to an array with one item
            var tmp = pagePaths;
            pagePaths = [tmp];
        }

        log.debug("Got list with " + pagePaths.length + " fixed page items");
        var iterableArray = {
            store: pagePaths,
            currentIdx: -1,
            next: function () {
                this.currentIdx++;
                return this.store[this.currentIdx];
            },
            hasNext: function () {
                return this.currentIdx + 1 < this.store.length;
            }
        };

        _deferredCollectSearchItems(iterableArray).then(function (itemList) {
            _addItems(resultPromise, itemList, listConfig);
        });

        return resultPromise.promise;
    };

    var _fetchList = function (listConfig) {
        var renderList = [];
        var resultPromise = {};
        var hasMore = false;

        if (listConfig.source == CONST.SOURCE_CHILDREN) {
            log.debug("Building list from child items");
            resultPromise = _addChildItems(listConfig);
        } else if (listConfig.source == CONST.SOURCE_DESCENDANTS) {
            log.debug("Building list from descendant items");
            resultPromise = _addDescendantItems(listConfig);
        } else if (listConfig.source == CONST.SOURCE_SEARCH) {
            // simple search
            log.debug("Building list from simple search");
            resultPromise = _addSimpleSearchItems(listConfig);
        } else if (listConfig.source == CONST.SOURCE_QUERYBUILDER) {
            // advanced query builder notation search
            log.debug("Building list from advanced query builder search");
            resultPromise = _addQueryBuilderSearchItems(listConfig);
        } else if (listConfig.source == CONST.SOURCE_TAGS) {
            log.debug("Building list from tag items");
            resultPromise = _addTagSearchItems(listConfig);
        } else {
            // fixed list of pages
            log.debug("Building list from fixed set of items");
            resultPromise = _addFixedPathsItems(renderList, listConfig);
        }

        return resultPromise;
    };

    var configProps = _initializeProperties();
    var listElement = configProps.ordered == true || configProps.ordered == "true"
        ? "ol" : "ul";
    return _fetchList(configProps).then(function (renderItems) {
        return {
            list: renderItems.renderList,
            element: listElement,
            type: configProps.type,
            isTouch: AuthoringUtils.isTouch,
            isEmpty: renderItems.renderList.length === 0,
            isPaginating: true,
            nextLink: _getNextPage(configProps, renderItems.hasMore),
            previousLink: _getPrevPage(configProps),
            accessibleNextDescriptionId: granite.resource.properties["accessibleNext"] ? "cq_" + _getGeneratedId() + "_next" : "",
            accessiblePreviousDescriptionId: granite.resource.properties["accessiblePrevious"] ? "cq_" + _getGeneratedId() + "_previous" : "",
            listId: _getGeneratedId(),
            pageStart: configProps.pageStart
        }
    });
});

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

use(function () {
	'use strict';


    var REDIRECT_RESOURCE_TYPE = "foundation/components/redirect";
    var PROP_REDIRECT_TARGET = "redirectTarget";

    var PROP_HIDE_IN_NAV = "hideInNav";
    var PROP_HIDE_SUB_IN_NAV = "hideSubItemsInNav";

    var PROP_NAV_ROOT = "navRoot";

    var items = [];

    var pageManager = resolver.adaptTo(com.day.cq.wcm.api.PageManager);
    var resourcePage = pageManager.getContainingPage(resource);
    var Calendar = Packages.java.util.Calendar;
    var currentYear = Calendar.getInstance().get(Calendar.YEAR);

    if (resourcePage.getPath().startsWith("/conf/")) {
        resourcePage = currentPage;
    }

    var isRoot = function(page) {
        var res = page.getContentResource(),
            vm = res.adaptTo(org.apache.sling.api.resource.ValueMap);

        return vm.get(PROP_NAV_ROOT, java.lang.Boolean);
    }

    var findRoot = function(resourcePage) {
        var currentPage = resourcePage;

        while(currentPage && !isRoot(currentPage)) {
            currentPage = currentPage.getParent();
        }

        return currentPage;
    }

    /**
     * Get list of pages
     * _root - root node to start listing from
     * level - how deep to get into the tree
     */
    var getPages = function(_root, level) {
        if (level === 0 || !_root) {
            return null;
        }
        var it = _root.listChildren(new Packages.com.day.cq.wcm.api.PageFilter());
        var _items = [], page, selected, pageContentResource, pageValueMap, pagePath, children;

        while (it.hasNext()) {
            page = it.next();
            pageContentResource = page.getContentResource();
            pageValueMap = pageContentResource.adaptTo(org.apache.sling.api.resource.ValueMap);

            if (pageValueMap.get(PROP_HIDE_IN_NAV, java.lang.Boolean)) {
                continue;
            }

            if (REDIRECT_RESOURCE_TYPE.equals(pageContentResource.getResourceType())) {
                page = resolveRedirect(pageValueMap);
            }

            selected = (currentNavPath && page && currentNavPath.contains(page.getPath()));

            _items.push({
                page: page,
                selected: selected,
                children: pageValueMap.get(PROP_HIDE_SUB_IN_NAV, java.lang.Boolean) ? [] : getPages(page, level - 1)
            });
        }

        return _items;
    };

    var resolveRedirect = function(pageValueMap) {
        var path = pageValueMap.get(PROP_REDIRECT_TARGET);
        return pageManager.getPage(path);
    };

    var getLanguages = function(root) {
        var items = [], page, selected,
            rootPath = root.getPath();

        var parent = root.getParent();
        if (parent == null) {
            return [];
        }
        var it = parent.listChildren(new Packages.com.day.cq.wcm.api.PageFilter());

        while (it.hasNext()) {
            page = it.next();

            items.push({
                path: page.getPath(),
                code: page.getName(),
                name: page.getTitle(),
                selected: rootPath.equals(page.getPath())
            });
        }

        return items;
    };


    var root = findRoot(resourcePage);
    var currentNavPath = currentPage && currentPage.getPath();
    var languageRoot = "#";
    var languages = [], currentLanguage = {};

    if (root) {
        items = getPages(root, 2);

        if (root.path.substring(0, 6) != "/conf/") {
            languageRoot = root.path + ".html";
        }

        languages = getLanguages(root);
        currentLanguage = {
            code: root.getName(),
            name: root.getTitle()
        };
    }


    var theme = properties.get("theme", "default");

    return {
        currentPath: currentPage.getPath(),
        items: items,
        theme: theme,
        languageRoot: languageRoot,
        languages: languages,
        currentLanguage: currentLanguage,
        year: currentYear
    };

});
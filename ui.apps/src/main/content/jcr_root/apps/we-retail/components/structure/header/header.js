/*
 *  Copyright 2015 Adobe Systems Incorporated
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

// Server-side JavaScript for the topnav logic
use(function () {
	// Checking if the Community area has been setup or not on this instance
	var communityEnabled = false;
	var communitySetup = '/bin/SetupCommunities?contentPath=/etc/community/we-retail';
	var communitySignin = '/content/we-retail/community/en/signin/j_security_check';
	var communityHome = '/content/we-retail/community/en';
	var communityBlog = '/content/we-retail/community/en/blog';
	var communityQA = '/content/we-retail/community/en/questions';
	var communityRoot = resolver.getResource('/content/we-retail/community');
	if (communityRoot != null) {
		communityEnabled = true;
	} else {
		communityHome = communitySetup + '&returnURL=' + communityHome + '.html#top';
		communitySignin = communitySetup + '.html#top';
		communityBlog = communitySetup + '&returnURL=' + communityBlog + '.html#top';
		communityQA = communitySetup + '&returnURL=' + communityQA + '.html#top';
	}
    var items = [];

    var pageManager = resolver.adaptTo(com.day.cq.wcm.api.PageManager);
    var resourcePage = pageManager.getContainingPage(resource);

    var rootLevel = properties.get("rootLevel") || currentStyle.get("rootLevel") || 3;

    var root = resourcePage.getAbsoluteParent(rootLevel);
    var currentNavPath = currentPage && currentPage.getPath();
    var languageRoot = "#";
    var languages = [], currentLanguage = {};

    /**
     * Get list of pages
     * _root - root node to start listing from
     * level - how deep to get into the tree
     */
    var getPages = function(_root, level) {
        if (level === 0) {
            return null;
        }
        var it = _root.listChildren(new Packages.com.day.cq.wcm.api.PageFilter());
        var _items = [], page, selected;

        while (it.hasNext()) {
            page = it.next();
            selected = (currentNavPath && currentNavPath.contains(page.getPath()));

            _items.push({
                page: page,
                selected: selected,
                children: getPages(page, level - 1)
            });
        }

        return _items;
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
		communityEnabled: communityEnabled,
		communityHome: communityHome,
		communitySignin: communitySignin,
		communityBlog: communityBlog,
		communityQA: communityQA,
		currentPath: currentPage.getPath(),
		items: items,
        theme: theme,
        languageRoot: languageRoot,
        languages: languages,
        currentLanguage: currentLanguage
    };
});
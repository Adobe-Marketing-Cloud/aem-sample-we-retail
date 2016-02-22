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
    var items = [];
    var root = currentPage.getAbsoluteParent(3);
    var currentNav = currentPage.getAbsoluteParent(4);
    var currentNavPath = currentNav && currentNav.getPath();

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
    }

    if (root) {
        items = getPages(root, 2);
    }

    var theme = properties.get("theme", "default");

    return {
        items: items,
        theme: theme,
        languageRoot: root
    };
});
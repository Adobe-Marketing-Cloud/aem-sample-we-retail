/*
 *  Copyright 2026 Adobe
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
(function ($) {
    'use strict';

    var DELAY = 300;
    var FOCUSED = 'cmp-search__item--is-focused';
    var KEY_ENTER = 13;
    var KEY_ESCAPE = 27;
    var KEY_UP = 38;
    var KEY_DOWN = 40;

    function createElement(tagName, className) {
        var element = document.createElement(tagName);
        element.className = className;
        return element;
    }

    function show(element, visible) {
        element.style.display = visible ? '' : 'none';
    }

    function isVisible(element) {
        return element.getClientRects().length > 0;
    }

    function markedTitle(title, term) {
        var titleElement = createElement('span', 'cmp-search__item-title');
        var pattern = new RegExp(term.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&'), 'gi');
        var start = 0;
        var match;

        while ((match = pattern.exec(title)) !== null) {
            var mark = createElement('mark', 'cmp-search__item-mark');
            mark.textContent = match[0];
            titleElement.appendChild(document.createTextNode(title.substring(start, match.index)));
            titleElement.appendChild(mark);
            start = match.index + match[0].length;
        }
        titleElement.appendChild(document.createTextNode(title.substring(start)));
        return titleElement;
    }

    function enhance(search) {
        var form = search.querySelector('.cmp-search__form');
        var input = search.querySelector('.cmp-search__input');
        var results = search.querySelector('.cmp-search__results');
        var clear = search.querySelector('.cmp-search__clear');
        var action = form ? form.getAttribute('action') || '' : '';
        var pagePathEnd = action.indexOf('.searchresults.json');

        if (!input || !results || pagePathEnd < 1) {
            return;
        }

        var endpoint = action.substring(0, pagePathEnd) + '.productsuggestions.json';
        var minLength = parseInt(search.getAttribute('data-cmp-min-length'), 10) || 3;
        var panel = createElement('div', 'we-SearchSuggestions');
        var productsHeading = createElement('div', 'we-SearchSuggestions-heading');
        var products = createElement('div', 'we-SearchSuggestions-products');
        var pagesHeading = createElement('div', 'we-SearchSuggestions-heading');
        var seeAll = createElement('a', 'cmp-search__item we-SearchSuggestions-all');
        var suggestions = null;
        var requestCount = 0;
        var requestedPages = 0;
        var closed = false;
        var timeout;

        results.parentNode.insertBefore(panel, results);
        [productsHeading, products, pagesHeading, results, seeAll].forEach(function (element) {
            panel.appendChild(element);
        });
        seeAll.setAttribute('role', 'option');
        seeAll.setAttribute('aria-selected', 'false');

        function update() {
            var hasProducts = products.children.length > 0;
            var hasPages = results.style.display !== 'none' && results.querySelector('.cmp-search__item') !== null;
            var hasResultsLink = !!(suggestions && suggestions.resultsUrl && suggestions.total > 0);
            var open = !closed && (hasProducts || hasPages || hasResultsLink);

            show(productsHeading, hasProducts);
            show(pagesHeading, hasProducts && hasPages);
            show(seeAll, hasResultsLink);
            panel.classList.toggle('is-open', open);
            input.setAttribute('aria-expanded', open ? 'true' : 'false');
        }

        function reset() {
            requestCount++;
            suggestions = null;
            requestedPages = 0;
            while (products.firstChild) {
                products.removeChild(products.firstChild);
            }
        }

        function render(data, term) {
            suggestions = data;
            productsHeading.textContent = data.labels.products;
            pagesHeading.textContent = data.labels.pages;
            seeAll.textContent = data.labels.seeAll;
            seeAll.setAttribute('href', data.resultsUrl || '#');
            data.products.forEach(function (product) {
                var link = createElement('a', 'cmp-search__item we-SearchSuggestions-product');
                link.setAttribute('href', product.url);
                link.setAttribute('role', 'option');
                link.setAttribute('aria-selected', 'false');
                link.appendChild(markedTitle(product.title, term));
                products.appendChild(link);
            });
            update();
        }

        function load(term, callback) {
            var request = ++requestCount;
            $.ajax({ url: endpoint, data: { search_query: term }, dataType: 'json' }).done(function (data) {
                if (request === requestCount) {
                    callback(data);
                }
            });
        }

        function suggest() {
            var term = input.value.trim();

            clearTimeout(timeout);
            reset();
            closed = false;
            update();
            if (term.length >= minLength) {
                timeout = setTimeout(function () {
                    load(term, function (data) {
                        render(data, term);
                    });
                }, DELAY);
            }
        }

        function openResults(term) {
            if (suggestions && suggestions.searchTerm === term) {
                if (suggestions.total > 0 && suggestions.resultsUrl) {
                    window.location.href = suggestions.resultsUrl;
                }
                return;
            }

            clearTimeout(timeout);
            reset();
            load(term, function (data) {
                if (data.total > 0 && data.resultsUrl) {
                    window.location.href = data.resultsUrl;
                } else {
                    render(data, term);
                }
            });
        }

        function focusItem(step) {
            var items = [].slice.call(panel.querySelectorAll('.cmp-search__item')).filter(isVisible);
            var focused = panel.querySelector('.' + FOCUSED);
            var index = items.indexOf(focused);
            var next = index === -1 ? (step > 0 ? 0 : items.length - 1) : Math.max(0, Math.min(items.length - 1, index + step));

            if (!items.length) {
                return;
            }
            if (focused) {
                focused.classList.remove(FOCUSED);
                focused.setAttribute('aria-selected', 'false');
            }
            items[next].classList.add(FOCUSED);
            items[next].setAttribute('aria-selected', 'true');
            items[next].scrollIntoView({ block: 'nearest' });
        }

        search.addEventListener('keydown', function (event) {
            var term = input.value.trim();
            var focused = panel.querySelector('.' + FOCUSED);

            if (event.target !== input) {
                return;
            }

            if (event.keyCode === KEY_ESCAPE) {
                closed = true;
                update();
            } else if (event.keyCode === KEY_ENTER && (term || focused)) {
                event.preventDefault();
                event.stopPropagation();
                if (focused && isVisible(focused)) {
                    window.location.href = focused.getAttribute('href');
                } else {
                    openResults(term);
                }
            } else if (event.keyCode === KEY_DOWN || event.keyCode === KEY_UP) {
                if (panel.classList.contains('is-open')) {
                    event.preventDefault();
                    event.stopPropagation();
                    focusItem(event.keyCode === KEY_DOWN ? 1 : -1);
                } else if (!suggestions || suggestions.searchTerm !== term) {
                    suggest();
                } else {
                    closed = false;
                    update();
                }
            }
        }, true);

        input.addEventListener('input', suggest);

        input.addEventListener('focus', function () {
            if (!suggestions || suggestions.searchTerm !== input.value.trim()) {
                suggest();
            } else {
                closed = false;
                update();
            }
        });

        if (clear) {
            clear.addEventListener('click', function () {
                clearTimeout(timeout);
                reset();
                update();
            });
        }

        document.addEventListener('click', function (event) {
            if (!search.contains(event.target)) {
                closed = true;
                update();
            }
        });

        panel.addEventListener('scroll', function () {
            var pages = results.querySelectorAll('.cmp-search__item').length;
            if (pages > requestedPages && panel.scrollTop + 2 * panel.clientHeight >= panel.scrollHeight) {
                requestedPages = pages;
                results.dispatchEvent(new Event('scroll'));
            }
        });

        new MutationObserver(update).observe(results, { attributes: true, attributeFilter: ['style'], childList: true });
    }

    $(function () {
        $('.we-search [data-cmp-is="search"]').each(function () {
            enhance(this);
        });
    });
})(jQuery);

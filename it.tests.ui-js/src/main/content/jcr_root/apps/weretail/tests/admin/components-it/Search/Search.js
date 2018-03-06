/*
 *  Copyright 2018 Adobe Systems Incorporated
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

;(function(h, $){
    // shortcuts
    var c = window.CQ.WeRetailIT.commons;
    var search = window.CQ.WeRetailIT.Search;

    var searchValue = "Women";

    var selectors = {
        searchIcon: '[data-target=\'#navbar-search\']',
        dialog: '.we-SearchModal.modal.fade.in',
        inputFulltext: 'input[name=\'fulltext\']',
        searchItem: '.cmp-search__item'
    };

    /**
     * Before Test Case
     */
    search.tcExecuteBeforeTest = function() {
        return new TestCase("Setup Before Test")
        // common set up
            .execTestCase(c.tcExecuteBeforeTest)
            // open the start page in publish mode
            .navigateTo("/content/we-retail/language-masters/en.html?wcmmode=disabled");
    };

    /**
     * After Test Case
     */
    search.tcExecuteAfterTest = function() {
        return new TestCase("Clean up after Test")
        // common clean up
            .execTestCase(c.tcExecuteAfterTest);
    };


    /**
     * Test: Execute a simple search on the We.Retail start page
     */
    search.tcExecuteSimpleSearch = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Open Search Modal and execute simple search', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            .click(selectors.searchIcon)
            .asserts.isTrue(function () {
                return h.find(selectors.dialog).size() == 1
            })
            .fillInput(selectors.inputFulltext, searchValue)
            .asserts.isTrue(function () {
                return h.find(selectors.searchItem).size() >= 1
            });
    };

    var tcExecuteBeforeTest = search.tcExecuteBeforeTest();
    var tcExecuteAfterTest = search.tcExecuteAfterTest();

    /**
     * The main test suite for the Search Component
     */
    new h.TestSuite('We.Retail Tests - Search', {path: '/apps/weretail/tests/admin/components-it/Search/Search.js',
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})
        .addTestCase(search.tcExecuteSimpleSearch(tcExecuteBeforeTest, tcExecuteAfterTest));
}(hobs, jQuery));
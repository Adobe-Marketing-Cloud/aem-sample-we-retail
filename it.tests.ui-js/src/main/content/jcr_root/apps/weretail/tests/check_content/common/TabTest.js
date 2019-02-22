/*
 *  Copyright 2019 Adobe Systems Incorporated
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
;(function(h) {

    var selectors = {
        tab: {
            self: ".cmp-tabs",
            list: {
                items: ".cmp-tabs__tablist li"
            },
            tabpanel: {
                active: ".cmp-tabs__tabpanel--active"
            },
        },
        contentfragment_storelocation_value: {
            self: ".contentfragment .cmp-contentfragment__element--storelocation .cmp-contentfragment__element-value"
        }
    };

    window.CQ.WeRetailIT.TabItemsTest = function (h, $, count) {
        return new h.TestCase("Check if tab list items are visible")
            // check if tab is visible
            .asserts.visible(selectors.tab.self, true)
            // check number of tab list items
            .asserts.isTrue(function () {
                return window.CQ.WeRetailIT.checkNumberOfItems(h, selectors.tab.list.items, count)
            })
    };

    window.CQ.WeRetailIT.TabListItemsNavigationTest = function (h, $) {
        return new h.TestCase("Check tab list items navigation")

            // clicking to 'San Francisco' tab
            .click(selectors.tab.list.items+":contains('San Francisco')")
            .asserts.isTrue(function () {
                return h.find(selectors.tab.tabpanel.active +" "+ selectors.contentfragment_storelocation_value.self).text().trim() === "WeRetail San Francisco"
            })

            // clicking to 'San Jose' tab
            .click(selectors.tab.list.items+":contains('San Jose')")
            .assert.isTrue(function(){
                return h.find(selectors.tab.tabpanel.active +" "+ selectors.contentfragment_storelocation_value.self).text().trim() === "WeRetail San Jose"})

            //clicking to 'New York' tab
            .click(selectors.tab.list.items+":contains('New York')")
            .assert.isTrue(function(){
                return h.find(selectors.tab.tabpanel.active +" "+ selectors.contentfragment_storelocation_value.self).text().trim() === "WeRetail New York"})
    }
})(hobs);

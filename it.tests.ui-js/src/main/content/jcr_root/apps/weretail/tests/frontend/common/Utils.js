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

/**
 * Check an element matching the given selector points to a specified image
 *
 * @param hobs Hobbes helper
 * @param selector CSS selector
 * @param src Image SRC
 * @returns {boolean} True if at least one element matching the given selector points to the image, false otherwise
 */
window.CQ.WeRetailIT.checkImage = function (hobs, selector, src) {
    var res = hobs.find(selector + "[src$='" + src + "'");
    return res && (res.length > 0);
};

/**
 * Check an element matching the given selector contains an expected text
 *
 * @param hobs Hobbes helper
 * @param selector CSS selector
 * @param text Text to look for
 *
 * @returns {boolean} True if at least one element matching the given selector contains the text, false otherwise
 */
window.CQ.WeRetailIT.checkText = function (hobs, selector, text) {
    var res = hobs.find(selector + ":contains(" + text + ")");
    return res && (res.length > 0);
};

/**
 * Check the given selector returns an exact array of text items.
 *
 * @param hobs Hobbes helper
 * @param selector CSS selector
 * @param items Array of items to check against
 *
 * @returns {boolean} True if the selector returns the exact array of text items, false otherwise
 */
window.CQ.WeRetailIT.checkItems = function(hobs, selector, items) {
    var foundItems = hobs.find(selector);
    if (foundItems.length != items.length) {
        return false;
    }
    foundItems.each(function(ix, val) {
        if (items[ix] != $(val).text().trim()) {
            return false;
        }
    });
    return true;
};

/**
 * Check if all the items in the provided array are found in the page using the given selector, irrespective of order
 *
 * @param hobs Hobbes helper
 * @param selector CSS selector
 * @param items Array of items to check for
 *
 * @returns {boolean} True if all the items from the array were found using the selector, false otherwise
 */
window.CQ.WeRetailIT.checkItemsFound = function(hobs, selector, items) {
    if (!items || items.length == 0) {
        return true;
    }
    var foundItems = hobs.find(selector);
    if (!foundItems || foundItems.length < items.length) {
        return false;
    }
    for (var i = 0; i < items.length; i++) {
        var found = true;
        for (var j = 0; j < foundItems.length; j ++) {
            if (items[i] == foundItems[j].text.trim()) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
    }
    return true;
};

/**
 * Check the given selector returns an exact number of items
 *
 * @param hobs Hobbes helper
 * @param selector CSS selector
 * @param numberOfItems Expected number of items
 *
 * @returns {boolean} True if the selector returns the exact number of items, false otherwise
 */
window.CQ.WeRetailIT.checkNumberOfItems = function (hobs, selector, numberOfItems) {
    return hobs.find(selector).length == numberOfItems;
};
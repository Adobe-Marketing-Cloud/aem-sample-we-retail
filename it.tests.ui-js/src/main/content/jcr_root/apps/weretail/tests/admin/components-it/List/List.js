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

;(function(h, $){
    // shortcuts
    var c = window.CQ.WeRetailIT.commons;
    var list = window.CQ.WeRetailIT.List;

    var searchValue = "Victor Sullivan";
    var tag1 = "ellie";
    var tag2 = "joel";
    var description = "This is a child page"

    var selectors = {
        item: '.cmp-list__item-link'
    };

    /**
     * Before Test Case
     */
    list.tcExecuteBeforeTest = function() {
        return new TestCase("Setup Before Test")
        // common set up
            .execTestCase(c.tcExecuteBeforeTest)

            // add 2 tags
            .execFct(function (opts, done) {
                c.addTag(tag1, done);
            })
            .execFct(function (opts, done) {
                c.addTag(tag2, done);
            })

            // create a separate parent page
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage, 'page_' + Date.now(), "parentPath", done)
            })

            // add page 1
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("parentPath")(opts), 'page_1', "page1Path", done)
            })
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['cq:tags'] = tag1;
                data['jcr:description'] = description;
                c.editNodeProperties(h.param("page1Path")() + "/jcr:content", data, done);
            })
            // add page 2
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("parentPath")(opts), 'page_2', "page2Path", done)
            })
            // add a text component
            .execFct(function (opts, done) {
                c.addComponent(c.rtList, h.param("page2Path")(opts) + c.relParentCompPath, "text1Path", done)
            })
            // set some text in the text component
            .execFct(function (opts, done) {
                var data = {};
                data.text = searchValue;
                c.editNodeProperties(h.param("text1Path")(), data, done);
            })
            // create subpage for page 2
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("page2Path")(opts), 'sub_2_1', "page21Path", done)
            })
            // create second sub page for page 2
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("page2Path")(opts), 'sub_2_2', "page22Path", done)
            })
            // add page 3
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("parentPath")(opts), 'page_3', "page3Path", done)
            })
            // set 2 tags on the page
            .execFct(function (opts, done) {
                var data = {};
                data['cq:tags'] = [tag1, tag2];
                c.editNodeProperties(h.param("page3Path")() + "/jcr:content", data, done);
            })
            // create page 4
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("parentPath")(opts), 'page_4', "page4Path", done)
            })
            // create a sub page for page 4
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("page4Path")(opts), 'sub_4_1', "page41Path", done)
            })
            // add a text component
            .execFct(function (opts, done) {
                c.addComponent(c.rtText, h.param("page41Path")(opts) + c.relParentCompPath, "text2Path", done)
            })
            // set some text value
            .execFct(function (opts, done) {
                var data = {};
                data.text = searchValue;
                c.editNodeProperties(h.param("text2Path")(), data, done);
            })
            // create page 5
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("parentPath")(opts), 'page_5', "page5Path", done)
            })
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['cq:tags'] = tag2;
                c.editNodeProperties(h.param("page5Path")() + "/jcr:content", data, done);
            })

            // create the test page containing the list component, store page path in 'testPagePath'
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage, 'page_' + Date.now(), "testPagePath", done)
            })

            // add the component, store component path in 'cmpPath'
            .execFct(function (opts, done) {
                c.addComponent(c.rtList, h.param("testPagePath")(opts) + c.relParentCompPath, "cmpPath", done)
            })
            // open the new page in the editor
            .navigateTo("/editor.html%testPagePath%.html");
    };

    /**
     * After Test Case
     */
    list.tcExecuteAfterTest = function() {
        return new TestCase("Clean up after Test")
            // common clean up
            .execTestCase(c.tcExecuteAfterTest)
            // delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("testPagePath")(opts), done);
            })
            // delete the separate test page tree we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("parentPath")(opts), done);
            });
    };


    /**
     * Test: Build a list using direct child pages
     */
    list.tcCreateListDirectChildren = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('List of direct Children', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // create 3 direct sub pages
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("testPagePath")(opts), 'direct_1', "subpage1Path", done)
            })
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("testPagePath")(opts), 'direct_2', "subpage2Path", done)
            })
            .execFct(function (opts, done) {
                c.createPage(c.template, h.param("testPagePath")(opts), 'direct_3', "subpage3Path", done)
            })

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // default setting is to build list using 'child pages', empty 'parent page' and 'child depth' = 1,
            // so we only need to save

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('direct_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('direct_2')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('direct_3')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a list using child pages from a different location
     */
    list.tcCreateListChildren = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('List of Children', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_2')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_3')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_4')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_5')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a list using child pages and sub child pages
     */
    list.tcListSubChildren = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('List with Subchildren', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_2')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('sub_2_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('sub_2_2')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_3')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_4')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('sub_4_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_5')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a fixed list
     */
    list.tcCreateFixedList = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Create a fixed List', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // select Fixed List
            .execTestCase(c.tcUseDialogSelect("./listFrom", "static"))

            // click the button
            .click("coral-multifield[data-granite-coral-multifield-name='./pages'] > button")
            .execTestCase(c.tcSelectInAutocomplete("[name='./pages']", "%page1Path%"))

            .click("coral-multifield[data-granite-coral-multifield-name='./pages'] > button")
            .execTestCase(c.tcSelectInAutocomplete("[name='./pages']:eq(1)", "%page21Path%"))

            .click("coral-multifield[data-granite-coral-multifield-name='./pages'] > button")
            .execTestCase(c.tcSelectInAutocomplete("[name='./pages']:eq(2)", "%page4Path%"))

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('sub_2_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_4')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a list using search
     */
    list.tcCreateListBySearch = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Create List using Search', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set the content path
            .execTestCase(c.tcUseDialogSelect("./listFrom", "search"))
            // set the search query
            .fillInput("input[name='./query']", searchValue)
            // set search location
            .execTestCase(c.tcSelectInAutocomplete("[name='./searchIn']", "%parentPath%"))

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_2')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('sub_4_1')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a list matching any tags defined
     */
    list.tcCreateListAnyTagsMatching = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('List with any Tags matching', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set the content path
            .execTestCase(c.tcUseDialogSelect("./listFrom", "tags"))
            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./tagsSearchRoot']", "%parentPath%"))
            // search for 2 tags
            .execTestCase(c.tcSelectInTags("[name='./tags']", tag1))
            .execTestCase(c.tcSelectInTags("[name='./tags']", tag2))
            // set the content path
            .execTestCase(c.tcUseDialogSelect("./tagsMatch", "any"))

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_1')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_3')", "#ContentFrame").size() == 1
            })
            .asserts.isTrue(function () {
                return h.find("span:contains('page_5')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: Build a list matching all tags defined
     */
    list.tcCreateListAllTagsMatching = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('List with all Tags matching', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set the content path
            .execTestCase(c.tcUseDialogSelect("./listFrom", "tags"))
            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./tagsSearchRoot']", "%parentPath%"))
            // search for 2 tags
            .execTestCase(c.tcSelectInTags("[name='./tags']", tag1))
            .execTestCase(c.tcSelectInTags("[name='./tags']", tag2))
            // set the content path
            .execTestCase(c.tcUseDialogSelect("./tagsMatch", "all"))

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if the correct pages are listed
            .asserts.isTrue(function () {
                return h.find("span:contains('page_3')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: order list by title
     */
    list.tcOrderByTitle = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Order List by Title', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // set order by title
            .execTestCase(c.tcUseDialogSelect("./orderBy", "title"))
            // set sort order to ascending
            .execTestCase(c.tcUseDialogSelect("./sortOrder", "asc"))
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if they are listed in the right order
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(0)", "#ContentFrame").text().trim() == "page_1";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(1)", "#ContentFrame").text().trim() == "page_2";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(2)", "#ContentFrame").text().trim() == "page_3";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(3)", "#ContentFrame").text().trim() == "page_4";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(4)", "#ContentFrame").text().trim() == "page_5";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(5)", "#ContentFrame").text().trim() == "sub_2_1";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(6)", "#ContentFrame").text().trim() == "sub_2_2";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(7)", "#ContentFrame").text().trim() == "sub_4_1";
            });
    };

    /**
     * Test: change ordering of a list to descending
     */
    list.tcChangeOrderingTitle = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Change Sort Order for Title', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // set order by title
            .execTestCase(c.tcUseDialogSelect("./orderBy", "title"))
            // set sort order to ascending
            .execTestCase(c.tcUseDialogSelect("./sortOrder", "desc"))
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // check if they are listed in the right order
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(0)", "#ContentFrame").text().trim() == "sub_4_1";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(1)", "#ContentFrame").text().trim() == "sub_2_2";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(2)", "#ContentFrame").text().trim() == "sub_2_1";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(3)", "#ContentFrame").text().trim() == "page_5";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(4)", "#ContentFrame").text().trim() == "page_4";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(5)", "#ContentFrame").text().trim() == "page_3";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(6)", "#ContentFrame").text().trim() == "page_2";
            })
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(7)", "#ContentFrame").text().trim() == "page_1";
            });
    };

    /**
     * Test: set max item
     */
    list.tcSetMaxItems = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Set Max Items', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // by default there should be 8
            .asserts.isTrue(function () {
                return h.find(".cmp-list li", "#ContentFrame").size() == 8;
            })

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set max Items to 4
            .fillInput("coral-numberinput[name='./maxItems'] > input", "4")
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // now it should only render 4 entries
            .asserts.isTrue(function () {
                return h.find(".cmp-list li", "#ContentFrame").size() == 4;
            });
    };

    /**
     * Test: order list by last modified date
     */
    list.tcOrderByLastModifiedDate = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Order by Last Modified Date', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // modify page 5
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['jcr:title'] = "Modified Page 5";
                c.editNodeProperties(h.param("page5Path")() + "/jcr:content", data, done);
            })

            .wait(3000)

            // modify page 1
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['jcr:title'] = "Modified Page 1";
                c.editNodeProperties(h.param("page1Path")() + "/jcr:content", data, done);
            })

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // set order by title
            .execTestCase(c.tcUseDialogSelect("./orderBy", "modified"))
            // set sort order to ascending
            .execTestCase(c.tcUseDialogSelect("./sortOrder", "asc"))
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // page 5 should be at 7th place
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(6)", "#ContentFrame").text().trim()=="Modified Page 5";
            })
            // page 1 should be at 8th place
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(7)", "#ContentFrame").text().trim()=="Modified Page 1";
            });
    };

    /**
     * Test: order list by last modified date
     */
    list.tcChangeOrderingDate = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Change Sort Order for Date', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

            // modify page 3
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['jcr:title'] = "Modified Page 3";
                c.editNodeProperties(h.param("page3Path")() + "/jcr:content", data, done);
            })

            .wait(3000)

            // modify page 1
            // set tag on the page
            .execFct(function (opts, done) {
                var data = {};
                data['jcr:title'] = "Modified Page 2";
                c.editNodeProperties(h.param("page2Path")() + "/jcr:content", data, done);
            })

            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))
            // set depth to 2
            .fillInput("coral-numberinput[name='./childDepth'] > input", "2")
            // set order by title
            .execTestCase(c.tcUseDialogSelect("./orderBy", "modified"))
            // set sort order to ascending
            .execTestCase(c.tcUseDialogSelect("./sortOrder", "desc"))
            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // page 2 should be at first place
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(0)", "#ContentFrame").text().trim()=="Modified Page 2";
            })
            // page 3 should be at second place
            .asserts.isTrue(function () {
                return h.find(".cmp-list li span:eq(1)", "#ContentFrame").text().trim()=="Modified Page 3";
            });
    };

    /**
     * Test: item settings - link items option
     */
    list.tcLinkItemsForList = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Link the items from a list', {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))

            .click("coral-tab-label:contains('Item Settings')")
            .click("input[name='./linkItems']")

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            .asserts.isTrue(function () {
                return h.find("a"+selectors.item+"[href*='page_1.html']", "#ContentFrame").size() == 1
            });
    };
    /**
     * Test: item settings - show description
     */
    list.tcShowDescriptionForList = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Show the list items's description", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))

            .click("coral-tab-label:contains('Item Settings')")
            .click("input[name='./showDescription']")

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            .asserts.isTrue(function () {
                return h.find("span:contains('This is a child page')", "#ContentFrame").size() == 1
            });
    };

    /**
     * Test: item settings - show date
     */
    list.tcShowDateForList = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Show the list items's date", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // open the configuration dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))

            // set parent page
            .execTestCase(c.tcSelectInAutocomplete("[name='./parentPage']", "%parentPath%"))

            .click("coral-tab-label:contains('Item Settings')")
            .click("input[name='./showModificationDate']")

            // close the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            .asserts.isTrue(function () {
                var date = new Date().toISOString().slice(0,10)
                return h.find("span:contains('"+date+"')", "#ContentFrame").size() >= 1
            });
    };

    var tcExecuteBeforeTest = list.tcExecuteBeforeTest();
    var tcExecuteAfterTest = list.tcExecuteAfterTest();

    /**
     * The main test suite for Text Component
     */
    new h.TestSuite('We.Retail Tests - List', {path: '/apps/weretail/tests/admin/components-it/List/List.js',
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(list.tcCreateListDirectChildren(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcCreateListChildren(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcListSubChildren(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcCreateFixedList(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcCreateListBySearch(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcCreateListAnyTagsMatching(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcCreateListAllTagsMatching(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcOrderByTitle(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcChangeOrderingTitle(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcOrderByLastModifiedDate(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcChangeOrderingDate(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcSetMaxItems(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcLinkItemsForList(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcShowDescriptionForList(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(list.tcShowDateForList(tcExecuteBeforeTest, tcExecuteAfterTest))
    ;

}(hobs, jQuery));
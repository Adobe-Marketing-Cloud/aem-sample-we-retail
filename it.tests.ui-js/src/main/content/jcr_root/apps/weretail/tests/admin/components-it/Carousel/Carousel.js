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
    var carousel = window.CQ.WeRetailIT.Carousel;

    var selectors = {
        editDialog: {
            childrenEditor: {
                self: ".cmp-childreneditor",
                addButton: "[data-cmp-hook-childreneditor='add']",
                removeButton: "button[handle='remove']",
                item: {
                    self: "coral-multifield-item",
                    first: "coral-multifield-item:first",
                    last: "coral-multifield-item:last",
                    input: "[data-cmp-hook-childreneditor='itemTitle']",
                    hiddenInput: "[data-cmp-hook-childreneditor='itemResourceType']"
                }
            }
        },
        insertComponentDialog: {
            self: ".InsertComponentDialog",
            components: {
                responsiveGrid: "coral-selectlist-item[value='/libs/wcm/foundation/components/responsivegrid']"
            }
        },
        editableToolbar: {
            self: "#EditableToolbar",
            actions: {
                insert: ".cq-editable-action[data-action='INSERT']",
                panelSelect: ".cq-editable-action[data-action='PANEL_SELECT']"
            }
        },
        panelSelector: {
            self: ".cmp-panelselector",
            item: ".cmp-panelselector__table [is='coral-table-row']"
        },
        overlay: {
            self: ".cq-Overlay"
        }
    };

    var policyName = "/carousel";
    var policyLocation = "weretail/components/content";
    var pageName = "page_" + Date.now();
    var pageVar = "testPagePath";

    /**
     * Before Test Case
     */
    carousel.tcExecuteBeforeTest = function() {
        return new h.TestCase("Create Sample Content")
            // common set up
            .execTestCase(c.tcExecuteBeforeTest)
            // create the test page, store page path in 'testPagePath'
            .execFct(function (opts,done) {
                c.createPage(c.template, c.rootPage, pageName, pageVar, done)
            })

            .execFct(function(opts, done) {
                // we need to set property cq:isContainer to true
                var data = {};
                data["cq:isContainer"] = "true";
                c.editNodeProperties(c.rtCarousel, data, done);
            })

            // add the component, store component path in 'cmpPath'
            .execFct(function (opts, done){
                c.addComponent(c.rtCarousel, h.param(pageVar)(opts) + c.relParentCompPath, "cmpPath", done)
            })
            // open the new page in the editor
            .navigateTo("/editor.html%"+pageVar+"%.html");
    };

    /**
     * After Test Case
     */
    carousel.tcExecuteAfterTest = function() {
        return new TestCase("Clean up after Test")
            // common clean up
            .execTestCase(c.tcExecuteAfterTest)
            // delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param(pageVar)(opts), done);
            });
    };

    /**
     * Create child items
     */
    carousel.tcCreateItems = function(selectors) {
        return new h.TestCase("Create child items")
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            .click(selectors.editDialog.childrenEditor.addButton)
            .wait(200)
            .click(selectors.insertComponentDialog.components.responsiveGrid)
            .wait(200)
            .fillInput(selectors.editDialog.childrenEditor.item.last + " " + selectors.editDialog.childrenEditor.item.input, "item0")
            .click(selectors.editDialog.childrenEditor.addButton)
            .wait(200)
            .click(selectors.insertComponentDialog.components.responsiveGrid)
            .wait(200)
            .fillInput(selectors.editDialog.childrenEditor.item.last + " " + selectors.editDialog.childrenEditor.item.input, "item1")
            .click(selectors.editDialog.childrenEditor.addButton)
            .wait(200)
            .click(selectors.insertComponentDialog.components.responsiveGrid)
            .wait(200)
            .fillInput(selectors.editDialog.childrenEditor.item.last + " " + selectors.editDialog.childrenEditor.item.input, "item2")
            // save the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            .wait(200);
    };

    /**
     * Test: Edit Dialog: Add child items
     */
    carousel.tcAddItems = function(tcExecuteBeforeTest, tcExecuteAfterTest, selectors) {
        return new h.TestCase("Edit Dialog : Add child items", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // create new items with titles
            .execTestCase(carousel.tcCreateItems(selectors))
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // verify that 3 items have been created
            .asserts.isTrue(function() {
                var children = h.find(selectors.editDialog.childrenEditor.item.input);
                return children.size() === 3 &&
                    $(children[0]).val() === "item0" &&
                    $(children[1]).val() === "item1" &&
                    $(children[2]).val() === "item2";
            })
            // save the edit dialog
            .execTestCase(c.tcSaveConfigureDialog);
    };

    /**
     * Test: Edit Dialog : Remove child items
     */
    carousel.tcRemoveItems = function(tcExecuteBeforeTest, tcExecuteAfterTest, selectors) {
        return new h.TestCase("Edit Dialog : Remove child items", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // create new items with titles
            .execTestCase(carousel.tcCreateItems(selectors))
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // remove the first item
            .click(selectors.editDialog.childrenEditor.item.first + " " + selectors.editDialog.childrenEditor.removeButton)
            .wait(200)
            .execTestCase(c.tcSaveConfigureDialog)
            .wait(200)
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // verify that the first item has been removed
            .asserts.isTrue(function() {
                var children = h.find(selectors.editDialog.childrenEditor.item.input);
                return children.size() === 2 &&
                    $(children[0]).val() === "item1" &&
                    $(children[1]).val() === "item2";
            })
            .execTestCase(c.tcSaveConfigureDialog);
    };

    /**
     * Test: Edit Dialog : Re-order children
     */
    carousel.tcReorderItems = function(tcExecuteBeforeTest, tcExecuteAfterTest, selectors) {
        return new h.TestCase("Edit Dialog : Re-order children", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // create new items with titles
            .execTestCase(carousel.tcCreateItems(selectors))
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // move last item before the first one
            .execFct(function(opts, done) {
                var $first = h.find(selectors.editDialog.childrenEditor.item.first);
                var $last = h.find(selectors.editDialog.childrenEditor.item.last);
                $last.detach().insertBefore($first);
                done(true);
            })
            // save the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            .wait(200)
            // open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // verify the new order
            .asserts.isTrue(function() {
                var children = h.find(selectors.editDialog.childrenEditor.item.input);
                return children.size() === 3 &&
                    $(children[0]).val() === "item2" &&
                    $(children[1]).val() === "item0" &&
                    $(children[2]).val() === "item1";
            })
            .execTestCase(c.tcSaveConfigureDialog);
    };

    /**
     * Test: Panel Select
     */
    carousel.tcPanelSelect = function(tcExecuteBeforeTest, tcExecuteAfterTest, selectors) {
        return new h.TestCase("Panel Select", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
            // open the toolbar
            .click(selectors.overlay.self + "[data-path='%cmpPath%']")
            .asserts.visible(selectors.editableToolbar.self)

            // verify that initially no panel select action is available
            .asserts.visible(selectors.editableToolbar.actions.panelSelect, false)

            // create new items with titles
            .execTestCase(carousel.tcCreateItems(selectors))

            // open the toolbar
            .click(selectors.overlay.self + "[data-path='%cmpPath%']")
            .asserts.visible(selectors.editableToolbar.self)

            // verify the panel select action is available
            .asserts.visible(selectors.editableToolbar.actions.panelSelect)

            // open the panel selector and verify it's open
            .click(selectors.editableToolbar.actions.panelSelect)
            .asserts.visible(selectors.panelSelector.self)

            // verify that 3 items are available in the panel selector and the correct titles are visible
            .asserts.isTrue(function() {
              var items = h.find(selectors.panelSelector.item);
              return items.size() === 3 &&
                $(items[0]).is(selectors.panelSelector.item + ":contains(item0)") &&
                $(items[1]).is(selectors.panelSelector.item + ":contains(item1)") &&
                $(items[2]).is(selectors.panelSelector.item + ":contains(item2)");
            })

            // click elsewhere and verify an out of area click closes the panel selector
            .click(selectors.overlay.self + " [data-path^='%cmpPath%/item']")
            .asserts.visible(selectors.panelSelector.self, false);
    };

    /**
     * Test: Allowed components
     */
    carousel.tcAllowedComponents = function(tcExecuteBeforeTest, tcExecuteAfterTest, selectors, policyName, policyLocation, policyPath, policyAssignmentPath, pageRT) {
        return new h.TestCase("Allowed components", {
            execAfter: tcExecuteAfterTest
        })
            // add a policy for carousel component
            .execFct(function (opts, done) {
                var data = {};
                data["jcr:title"] = "New Policy";
                data["sling:resourceType"] = "wcm/core/components/policy/policy";
                data["components"] = c.rtTeaser;

                c.createPolicy(policyName + "/new_policy", data, "policyPath", done, policyPath);
            })

            .execFct(function (opts, done) {
                var data = {};
                data["cq:policy"] = policyLocation + policyName + "/new_policy";
                data["sling:resourceType"] = "wcm/core/components/policies/mapping";

                c.assignPolicy(policyName, data, done, policyAssignmentPath);
            }, {after: 1000})

            .execFct(function (opts, done) {
                // we need to set property cq:isContainer to true
                var data = {};
                data["cq:isContainer"] = "true";
                c.editNodeProperties(c.rtCarousel, data, done);
            })

            .execFct(function (opts,done) {
                c.createPage(c.template, c.rootPage, pageName, pageVar, done)
            })

            .execFct(function (opts, done) {
                c.addComponent(c.rtCarousel, h.param(pageVar)(opts) + c.relParentCompPath, "cmpPath", done);
            })
            .navigateTo("/editor.html%" + pageVar + "%.html")

            .click(selectors.overlay.self + "[data-path='%cmpPath%/*']")

            // make sure its visible
            .asserts.visible(selectors.editableToolbar.self)
            // click on the 'insert component' button
            .click(selectors.editableToolbar.actions.insert)
            // verify teaser is in the list of allowed components
            .asserts.visible("coral-selectlist-item[value='/apps/"+c.rtTeaser+"']")

            .execFct(function (opts, done) {
                c.deletePolicy(policyName, done, policyPath);
            })
            .execFct(function (opts, done) {
                c.deletePolicyAssignment(policyName, done, policyAssignmentPath);
            });
    };

    var tcExecuteBeforeTest = carousel.tcExecuteBeforeTest();
    var tcExecuteAfterTest = carousel.tcExecuteAfterTest();

    /**
     * The main test suite.
     */
    new h.TestSuite("We.Retail Tests - Carousel", {path:"/apps/weretail/tests/admin/components-it/Carousel.js", register: true,
        execBefore: c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(carousel.tcAddItems(tcExecuteBeforeTest, tcExecuteAfterTest, selectors))
        .addTestCase(carousel.tcRemoveItems(tcExecuteBeforeTest, tcExecuteAfterTest, selectors))
        .addTestCase(carousel.tcReorderItems(tcExecuteBeforeTest, tcExecuteAfterTest, selectors))
        .addTestCase(carousel.tcPanelSelect(tcExecuteBeforeTest, tcExecuteAfterTest, selectors))
        .addTestCase(carousel.tcAllowedComponents(tcExecuteBeforeTest, tcExecuteAfterTest, selectors, policyName, policyLocation,
            c.policyPath, c.policyAssignmentPath, "core/wcm/tests/components/test-page-v2"));

}(hobs, jQuery));

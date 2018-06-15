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
    var heroimage = window.CQ.WeRetailIT.HeroImage;
    heroimage.resourceType = c.rtHeroImage;

    /**
     * Before Test Case
     */
    heroimage.tcExecuteBeforeTest = function() {
        return new TestCase("Setup Before Test")
            // common set up
            .execTestCase(c.tcExecuteBeforeTest)
            // create the test page, store page path in 'testPagePath'
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage,'page_' + Date.now(), "testPagePath", done)
            })
            // add the component, store component path in 'cmpPath'
            .execFct(function (opts, done){
                c.addComponent(heroimage.resourceType, h.param("testPagePath")(opts)+c.relParentCompPath, "cmpPath", done)
            })
            // open the new page in the editor
            .navigateTo("/editor.html%testPagePath%.html");
    };

    /**
     * After Test Case
     */
    heroimage.tcExecuteAfterTest = function () {
        return new TestCase("Clean up after Test")
        // common clean up
            .execTestCase(c.tcExecuteAfterTest)
            // delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("testPagePath")(opts), done);
            });
    };

    /**
     * Test: check the dialog tabs
     */
    heroimage.tcCheckTabs = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Check Tabs',{
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

        // open the config dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // check the number of tabs
            .asserts.isTrue(function () {
                return h.find("coral-dialog .cmp-image__editor coral-tab").size() == 2
            })
            // check the tabs labels
            .asserts.isTrue(function () {
                var tabLabels = h.find("coral-dialog .cmp-image__editor coral-tab-label");
                return tabLabels[0].innerHTML === "Asset" && tabLabels[1].innerHTML === "Properties"
            });
    };

    var tcExecuteBeforeTest = heroimage.tcExecuteBeforeTest();
    var tcExecuteAfterTest = heroimage.tcExecuteAfterTest();

    /**
     * The main test suite for Hero Image Component
     */
    new h.TestSuite("We.Retail Tests - HeroImage", {path: '/apps/weretail/tests/admin/components-it/HeroImage/HeroImage.js',
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(heroimage.tcCheckTabs(tcExecuteBeforeTest, tcExecuteAfterTest))
    ;

}(hobs, jQuery));

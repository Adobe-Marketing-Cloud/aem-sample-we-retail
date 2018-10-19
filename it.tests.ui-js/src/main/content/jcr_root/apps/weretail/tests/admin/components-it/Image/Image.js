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
    var image = window.CQ.WeRetailIT.Image;

    var imageName = "we-retail-instore-logo.png";
    var testImagePath = "/content/dam/we-retail-screens/" + imageName;
    var altText = "Return to Arkham";
    var captionText = "The Last Guardian";

    var selectors = {
        cmp: {
            image: {
                self: '.cmp-image',
                image: '.cmp-image__image',
                title: '.cmp-image__title',
                dialog: {
                    metadataTab: 'coral-tab:contains("Metadata")',
                    isDecorative: 'input[name="./isDecorative"]',
                    alt: 'input[name="./alt"]',
                    altValueFromDAM: 'input[type="checkbox"][name="./altValueFromDAM"]',
                    title: 'input[name="./jcr:title"]',
                    titleValueFromDAM: 'input[type="checkbox"][name="./titleValueFromDAM"]',
                    displayPopupTitle: 'input[name="./displayPopupTitle"'
                }
            }
        },
        assetFinder: {
            search: '#assetsearch'
        }
    };

    /**
     * Before Test Case
     */
    image.tcExecuteBeforeTest = function() {
        return new TestCase("Setup Before Test")
            // common set up
            .execTestCase(c.tcExecuteBeforeTest)
            // create the test page, store page path in 'testPagePath'
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage,'page_' + Date.now(), "testPagePath", done)
            })
            // add the component, store component path in 'cmpPath'
            .execFct(function (opts, done){
                c.addComponent(c.rtImage, h.param("testPagePath")(opts)+c.relParentCompPath, "cmpPath", done)
            })
            // open the new page in the editor
            .navigateTo("/editor.html%testPagePath%.html");
    };


    /**
     * After Test Case
     */
    image.tcExecuteAfterTest = function () {
        return new TestCase("Clean up after Test")
        // common clean up
            .execTestCase(c.tcExecuteAfterTest)
            // delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("testPagePath")(opts), done);
            });
    };

    image.tcDragImage = function () {
        return new TestCase('Drag Asset')
            .execTestCase(c.tcOpenConfigureDialog('cmpPath'))
            .execFct(function (opts, done) {
                c.openSidePanel(done);
            })
            // search the image
            .fillInput(selectors.assetFinder.search, imageName, {after:5000})

            .cui.dragdrop('coral-card.cq-draggable[data-path="' + testImagePath + '"]', 'coral-fileupload[name="./file"')
            .execTestCase(c.closeSidePanel);
    };

    /**
     * Test: minimal properties
     */
    image.tcSetMinimalProps = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new TestCase("Set Image and Alt Text")
            .execFct(function (opts,done) {c.openSidePanel(done);})

            // search the image
            .fillInput(selectors.assetFinder.search, imageName, {after:5000})

            // drag'n'drop the test image
            .cui.dragdrop("coral-card.cq-draggable[data-path='" + testImagePath + "']","coral-fileupload[name='./file'")
            //open the Metadata tab
            .click(selectors.cmp.image.dialog.metadataTab)
            .click(selectors.cmp.image.dialog.altValueFromDAM)
            // set mandatory alt text
            .fillInput(selectors.cmp.image.dialog.alt, altText)

            // close the side panel
            .execTestCase(c.closeSidePanel);
    };

    /**
     * Test: add image
     */
    image.tcAddImage = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Add an Image', {
                execBefore: tcExecuteBeforeTest ,
                execAfter: tcExecuteAfterTest
            }
        )
            .execTestCase(image.tcDragImage())
            .execTestCase(c.tcSaveConfigureDialog)
            .asserts.isTrue(function () {
                return h.find(selectors.cmp.image.image + '[src*="' + h.param('testPagePath')() +
                    '/_jcr_content/root/responsivegrid/image.coreimg."]', '#ContentFrame').size() === 1;
            });
    };

    /**
     * Test: set Alt Text and Title
     */
    image.tcAddAltTextAndTitle = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Set Alt and Title Text', {
                execBefore: tcExecuteBeforeTest,
                execAfter : tcExecuteAfterTest
            }
        )
            .execTestCase(image.tcDragImage())
            .click(selectors.cmp.image.dialog.metadataTab)
            .wait(500)
            .click(selectors.cmp.image.dialog.altValueFromDAM)
            .wait(200)
            .click(selectors.cmp.image.dialog.titleValueFromDAM)
            .wait(200)
            .fillInput(selectors.cmp.image.dialog.alt, altText)
            .fillInput(selectors.cmp.image.dialog.title, captionText)
            .execTestCase(c.tcSaveConfigureDialog)
            .asserts.isTrue(function () {
                return h.find(selectors.cmp.image.image + '[src*="' + h.param('testPagePath')() +
                    '/_jcr_content/root/responsivegrid/image.coreimg."][alt="' + altText + '"][title="' + captionText + '"]', "#ContentFrame").size() === 1;
            });
    };

    image.tcDisableCaptionPopup = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Disable caption popup', {
                execBefore: tcExecuteBeforeTest,
                execAfter : tcExecuteAfterTest
            }
        )
            .execTestCase(image.tcDragImage())
            .click(selectors.cmp.image.dialog.metadataTab)
            .wait(500)
            .click(selectors.cmp.image.dialog.altValueFromDAM)
            .wait(200)
            .click(selectors.cmp.image.dialog.titleValueFromDAM)
            .wait(200)
            .click(selectors.cmp.image.dialog.displayPopupTitle)
            .fillInput(selectors.cmp.image.dialog.alt, altText)
            .fillInput(selectors.cmp.image.dialog.title, captionText)
            .execTestCase(c.tcSaveConfigureDialog)
            .asserts.isTrue(function () {
                return h.find(selectors.cmp.image.image + '[src*="' + h.param('testPagePath')() +
                    '/_jcr_content/root/responsivegrid/image.coreimg."][alt="' + altText + '"]', '#ContentFrame').size() === 1 &&
                    h.find(selectors.cmp.image.title + ':contains("' + captionText + '")', '#ContentFrame').size() === 1;
            });
    };

    image.tcSetImageAsDecorative = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Set Image as decorative',{
                execBefore: tcExecuteBeforeTest,
                execAfter: tcExecuteAfterTest
            }
        )
            .execTestCase(image.tcDragImage())
            .click(selectors.cmp.image.dialog.metadataTab)
            .wait(500)
            .execTestCase(c.tcSelectInAutocomplete("[name='./linkURL']", c.rootPage))
            .wait(500)
            .click(selectors.cmp.image.dialog.isDecorative)
            .wait(500)
            .execTestCase(c.tcSaveConfigureDialog)
            .config.changeContext(c.getContentFrame)
            .asserts.isTrue(function () {
                return h.find(selectors.cmp.image.image).attr('alt') === '' && h.find(selectors.cmp.image.link).size() === 0;
            });
    };

    /**
     * Test: set link on image
     */
    image.tcSetLink = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase('Set Link',{
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest})

        // open the config dialog
            .execTestCase(c.tcOpenConfigureDialog("cmpPath"))
            // set image and alt text
            .execTestCase(image.tcSetMinimalProps(tcExecuteBeforeTest, tcExecuteAfterTest))
            // enter the link
            .execTestCase(c.tcSelectInAutocomplete("[name='./linkURL']", c.rootPage))
            // save the dialog
            .execTestCase(c.tcSaveConfigureDialog)

            // switch to content frame
            .config.changeContext(c.getContentFrame)
            // click on the image
            .click(selectors.cmp.image.image, {expectNav: true})
            // go back to top frame
            .config.resetContext()
            // check if the url is correct
            .asserts.isTrue(function(){
                return hobs.context().window.location.pathname.endsWith(c.rootPage + ".html")
            });
    };

    /**
     * v1 specifics
     */

    var tcExecuteBeforeTest = image.tcExecuteBeforeTest();
    var tcExecuteAfterTest = image.tcExecuteAfterTest();

    /**
     * The main test suite for Image Component
     */
    new h.TestSuite("We.Retail Tests - Image", {path: '/apps/weretail/tests/admin/components-it/Image/Image.js',
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(image.tcAddImage(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(image.tcAddAltTextAndTitle(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(image.tcSetLink(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(image.tcDisableCaptionPopup(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(image.tcSetImageAsDecorative(tcExecuteBeforeTest, tcExecuteAfterTest))
    ;

}(hobs, jQuery));

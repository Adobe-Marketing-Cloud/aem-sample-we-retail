/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

/**
 * Tests for the core page component.
 */
;(function(h, $){

    // shortcut
    var c = window.CQ.WeRetailIT.commons;
    var page = window.CQ.WeRetailIT.Page;

    var tag1 = "TestTags : TestTag1";
    var tag2 = "TestTags : TestTag2";
    var pageTitle = "This is the page title";
    var navTitle = "This is the navigation title";
    var subtitle = "This is the page subtitle";
    var description = "This is the page description";
    var vanityURL = "test/test-Page-URL";
    var language = "Romanian";
    var design = "/etc/designs/we-retail/images/flags";
    var alias  = "This is an alias";
    var allowedTemplate = "allowedTemplates";
    var loginPage = "/content/we-retail/language-masters/en";
    var exportConfiguration = "/etc/contentsync/templates";
    var contextHubPath = "/etc/cloudsettings/default/contexthub/device";
    var configuration = "/conf/we-retail";

    var pageSelector={
        segmentPath: '/conf/we-retail/settings/wcm/segments'
    };


    /**
     * Test: open the page property.
     */
    page.openPageProperties = new h.TestCase("Open the page property")
    //select the page
        .execFct(function (opts, done) {
            c.setPageName(h.param("testPagePath")(opts), "testPageName", done);
        })
        .navigateTo("/mnt/overlay/wcm/core/content/sites/properties.html?item=%testPagePath%")
        //.click('coral-columnview-item:contains("%testPageName%") coral-columnview-item-thumbnail')
        //.click("button.cq-siteadmin-admin-actions-properties-activator",{expectNav:true})
    ;

    /**
     * Before Test Case
     */
    page.tcExecuteBeforeTest = function() {
        return new h.TestCase("Setup Before Test")
        // common set up
            .execTestCase(c.tcExecuteBeforeTest)
            // create the test page, store page path in 'testPagePath'
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage, 'page_' + Date.now(), "testPagePath", done)
            });
    };

    /**
     * After Test Case
     */
    page.tcExecuteAfterTest = function() {
        return new h.TestCase("Clean up after Test")
        // common clean up
            .execTestCase(c.tcExecuteAfterTest)
            // delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("testPagePath")(opts), done);
            });
    };

    /**
     * Test: Check the Basic Title and Tags options of a page properties.
     */
    page.tcBasicTitleAndTagsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Basic Title and Tags page properties", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Title and Tags' *****/

            //open the Basic tab
            .click("coral-tab-label:contains('Basic')")
            //check if the "Basic" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Basic')").size() === 1
            })

            //check the page title
            .assert.isTrue(function (opts) {
                return h.find("input[name='./jcr:title']").val() === h.param("testPageName")(opts)
            })
            //change the page title
            .fillInput("input[name='./jcr:title']", "Page")

            //add two tags
            .click("foundation-autocomplete.cq-ui-tagfield button")
            .click("coral-columnview-item-content[title='TestTags']")
            .click("coral-columnview-item:contains('TestTag1') coral-columnview-item-thumbnail",{after:1000})
            .click("coral-columnview-item:contains('TestTag2') coral-columnview-item-thumbnail",{after:1000})
            .click("button.granite-pickerdialog-submit",{after:1000})
            //check if tags were added
            .assert.exist("coral-taglist[name='./cq:tags'] coral-tag:contains('" + tag1 + "')")
            .assert.exist("coral-taglist[name='./cq:tags'] coral-tag:contains('" + tag2 + "')")

            //detele a tag
            .click("coral-taglist[name='./cq:tags'] coral-tag:contains('" + tag2 + "') > button")
            .assert.exist("coral-taglist[name='./cq:tags'] coral-tag:contains('" + tag2 + "')", false)

            //set the Hide in Navigation
            .click("input[name='./hideInNav']")

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Basic')")

            //check the page title
            .assert.isTrue(function (opts) {
                return h.find("input[name='./jcr:title']").val() === "Page"
            })
            //check if the tags were saved
            .assert.exist("coral-taglist[name='./cq:tags'] coral-tag:contains('" + tag1 + "')")

            //check if 'Hide in Navigation' is checked
            .assert.isTrue(function (opts) {
                return h.find("coral-checkbox[name='./hideInNav'][checked]")
            });
    };

    /**
     * Test: Check the Basic More titles and descriptions options of a page properties.
     */
    page.tcBasicTitlesAndDescriptionsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Basic Titles and Descriptions page properties", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'More Titles and Description' *****/

            //open the Basic tab
            .click("coral-tab-label:contains('Basic')")
            //check if the "Basic" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Basic')").size() === 1
            })

            .simulate("input[name='./pageTitle']", "key-sequence",
                {sequence: pageTitle})
            .simulate("input[name='./navTitle']", "key-sequence",
                {sequence: navTitle})
            .simulate("input[name='./subtitle']", "key-sequence",
                {sequence: subtitle})
            .simulate("textarea[name='./jcr:description']", "key-sequence",
                {sequence: description})

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Basic')")

            //check the saved data
            .assert.isTrue(function (opts) {
                return h.find("input[name='./pageTitle']").val() === pageTitle
            })
            .assert.isTrue(function (opts) {
                return h.find("input[name='./navTitle']").val() === navTitle
            })
            .assert.isTrue(function (opts) {
                return h.find("input[name='./subtitle']").val() === subtitle
            })
            .assert.isTrue(function (opts) {
                return h.find("textarea[name='./jcr:description").val() === description
            });
    };

    /**
     * Test: Check the Basic On/Off time options of a page properties.
     */
    page.tcBasicOnOffTimePageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Basic On/Off time page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for On/Off time *****/

            //open the Basic tab
            .click("coral-tab-label:contains('Basic')")
            //check if the "Basic" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Basic')").size() === 1
            })

            // open calendar for OnTime
            .click("coral-datepicker[name='./onTime'] button[handle='toggle']")
            // choose next month
            .click("coral-datepicker[name='./onTime'] button[handle='next']")
            // select first day
            .click("coral-datepicker[name='./onTime'] td a:contains('1'):eq(0)", {after: 1000})
            // open calendar for OffTime
            .click("coral-datepicker[name='./offTime'] button[handle='toggle']")
            // choose next month
            .click("coral-datepicker[name='./offTime'] button[handle='next']")
            // select second day
            .click("coral-datepicker[name='./offTime'] td a:contains('2'):eq(0)", {after: 1000})

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Basic')")

            //check the on time
            .assert.isTrue(function (opts) {
                return h.find("input[name='./onTime']").val() !== ""
            })
            //check the off time
            .assert.isTrue(function (opts) {
                return h.find("input[name='./offTime']").val() !== ""
            });
    };

    /**
     * Test: Check the Basic vanity URL options of a page properties.
     */
    page.tcBasicVanityUrlPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Basic Vanity URL page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Vanity URL' *****/

            //open the Basic tab
            .click("coral-tab-label:contains('Basic')")
            //check if the "Basic" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Basic')").size() === 1
            })

            //add a vanity url
            .click("coral-multifield[data-granite-coral-multifield-name='./sling:vanityPath'] > button")
            .simulate("input[name='./sling:vanityPath']", "key-sequence",
                {sequence: vanityURL})
            //delete a vanity Url
            .click("coral-multifield[data-granite-coral-multifield-name='./sling:vanityPath'] button[handle='remove']")
            //add again the vanity url
            .click("coral-multifield[data-granite-coral-multifield-name='./sling:vanityPath'] > button")
            .simulate("input[name='./sling:vanityPath']", "key-sequence",
                {sequence: vanityURL})

            //set the Redirect Vanity URL
            .click("input[name='./sling:redirect']")

            /*****  Check if data are saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Basic')")

            //check if the vanity url was saved
            .assert.isTrue(function (opts) {
                return h.find("input[name='./sling:vanityPath']").val() === vanityURL
            })
            //check if 'Redirect Vanity URL' is checked
            .assert.isTrue(function (opts) {
                return h.find("coral-checkbox[name='./sling:redirect'][checked]")
            });
    };

    /**
     * Test: Check the Advanced Settings options of a page properties.
     */
    page.tcAdvancedSettingsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Advanced Settings page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Settings' *****/

            //open the Advanced tab
            .click("coral-tab-label:contains('Advanced')", {after: 1000})
            //check if the "Advanced" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Advanced')").size() === 1
            })

            //test the Settings options

            //set the language
            .click("coral-select[name='./jcr:language'] > button")
            .click("coral-select[name='./jcr:language'] coral-selectlist-item:contains('" + language + "')")
            //set the desigh path
            .fillInput("foundation-autocomplete[name='./cq:designPath'] input[is='coral-textfield']", design)
            //set the alias
            .simulate("input[name='./sling:alias']", "key-sequence",
                {sequence: alias})
            // required when running with no pacing delay, otherwise designPath does not get saved.
            .wait(2000)
            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Advanced')", {after: 1000})

            //check the language
            .assert.isTrue(function (opts) {
                return h.find("coral-select[name='./jcr:language'] span:contains('" + language + "')")
            })
            //check the design
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:designPath']").val() === design
            })
            //check the alias
            .assert.isTrue(function (opts) {
                return h.find("input[name='./sling:alias']").val() === alias
            });
    };

    /**
     * Test: Check the Advanced Templates options of a page properties.
     */
    page.tcAdvancedTemplatesSettingsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Advanced Templates page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Settings' *****/

            //open the Advanced tab
            .click("coral-tab-label:contains('Advanced')", {after: 1000})
            //check if the "Advanced" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Advanced')").size() === 1
            })

            //test the template settings
            .click("coral-multifield[data-granite-coral-multifield-name='./cq:allowedTemplates'] > button")
            .simulate("input[name='./cq:allowedTemplates']", "key-sequence",
                {sequence: allowedTemplate})
            //detele the allowed template
            .click("coral-multifield[data-granite-coral-multifield-name='./cq:allowedTemplates'] button[handle='remove']")
            //add again the allowed template
            .click("coral-multifield[data-granite-coral-multifield-name='./cq:allowedTemplates'] > button")
            .simulate("input[name='./cq:allowedTemplates']", "key-sequence",
                {sequence: allowedTemplate})

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Advanced')", {after: 1000})

            //check the saved template
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:allowedTemplates']").val() === allowedTemplate
            });
    };

    /**
     * Test: Check the Advanced Authentication options of a page properties.
     */
    page.tcAdvancedAuthenticationPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Advanced Authentication page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Settings' *****/

            //open the Advanced tab
            .click("coral-tab-label:contains('Advanced')", {after: 1000})
            //check if the "Advanced" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Advanced')").size() === 1
            })

            //test the authentication requirement
            .click("input[name='./cq:authenticationRequired']")
            .fillInput("foundation-autocomplete[name='./cq:loginPath'] input[is='coral-textfield']", loginPage, {before: 1000})
            .click("button[value='" + loginPage + "']",{after:2000})

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Advanced')", {after: 1000})

            //check the Enable check
            .assert.isTrue(function (opts) {
                return h.find("coral-checkbox[name='./cq:authenticationRequired'] checked")
            })
            //check the login page
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:loginPath']").val() === loginPage
            });
    };

    /**
     * Test: Check the Advanced Export options of a page properties.
     */
    page.tcAdvancedExportPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Advanced Export page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Settings' *****/

            //open the Advanced tab
            .click("coral-tab-label:contains('Advanced')", {after: 1000})
            //check if the "Advanced" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Advanced')").size() === 1
            })

            //tests for the export options
            .fillInput("foundation-autocomplete[name='./cq:exportTemplate'] input[is='coral-textfield']", exportConfiguration)
            .wait(200)
            .click("button[value='" + exportConfiguration + "']")
            .wait(200)

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Advanced')", {after: 1000})

            //check the Export Configuration
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:exportTemplate']").val() === exportConfiguration
            });
    };

    /**
     * Test: Check the Thumbnail options of a page properties.
     */
    page.tcThumbnailPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Thumbnail page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Thumbnail')", {after: 1000})
            //check if the "Thumbnail" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Thumbnail')").size() === 1
            })

            .click("button:contains('Generate Preview')")
            /*
             .execFct(function(opts, done){

             // check defaults
             var maxRetries = 10;
             var timeout = 5000;
             // retry counter
             var retries = 0;

             // the polling function
             var poll = function () {

             if (h.find("button:contains('Revert')").is(":visible")) {
             done(true)
             }
             else {
             if (retries++ === maxRetries) {
             done(false, "getting the Revert button failed!");
             return;
             }
             // set for next retry
             setTimeout(poll, timeout);
             }
             };
             // start polling
             poll();
             })
             .click("button:contains('Revert')")
             .assert.visible("button:contains('Revert')", false)
             .assert.visible("button:contains('Upload Image')")
             */
            ;
    };

    /**
     * Test: Check the Social Media options of a page properties.
     */
    page.tcSocialMediaPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Social Media page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Social Media')", {after: 1000})
            //check if the "Social Media" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Social Media')").size() === 1
            })

            //test social media sharing
            .click("input[name='./socialMedia'][value='facebook']")
            .click("input[name='./socialMedia'][value='pinterest']")
            .click("foundation-autocomplete[name='./variantPath'] button[title='Open Selection Dialog']")
            .click("form.granite-pickerdialog-content button:contains('Cancel')")

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Social Media')", {after: 1000})

            //check if facebook is checked
            .assert.isTrue(function (opts) {
                return h.find("coral-checkbox[name='./socialMedia'][value='facebook'] checked")
            })
            //check if pinterest is checked
            .assert.isTrue(function (opts) {
                return h.find("coral-checkbox[name='./socialMedia'][value='pinterest'] checked")
            });
    };

    /**
     * Test: Check the Cloud Services options of a page properties.
     */
    page.tcCloudServicesPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Cloud Services page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Cloud Services')", {after: 1000})
            //check if the "Cloud Services" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Cloud Services')").size() === 1;
            })

            .click(".cq-CloudServices-container .cq-CloudServices-inheritance input", {after: 300})
            .click(".cq-CloudServices-container span:contains('Add Configuration')", {after: 300})
            .click("coral-selectlist-item span:contains('Cloud Proxy Configuration')", {after: 300})
            //detele the connection
            .click("button[data-title='Cloud Proxy Configuration']", {after: 300})
            .click(".cq-CloudServices-container span:contains('Add Configuration')", {after: 300})
            .click("coral-selectlist-item span:contains('Cloud Proxy Configuration')", {after: 300})
            .wait(300)

            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{before:2000,expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Cloud Services')", {after: 1000})

            .assert.isTrue(function () {
                return h.find("div.js-cq-CloudServices-currentConfig:contains('Cloud Proxy Configuration') coral-select[name='./cq:cloudserviceconfigs'] span:contains('Default IDS Pool')").size() === 1;
            });
    };

    /**
     * Test: Check the Personalization options of a page properties.
     */
    page.tcPersonalizationPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Personalization page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Personalization')", {after: 1000})
            //check if the "Personalization" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Personalization')").size() === 1
            })
                // cancel inheritance
            .click("coral-checkbox.js-ContextHub-inheritedFrom",{after:500})
            //set the contextHub path
            .fillInput("foundation-autocomplete[name='./cq:contextHubPath'] input[is='coral-textfield']", contextHubPath)
            //set the segments path
            .fillInput("foundation-autocomplete[name='./cq:contextHubSegmentsPath'] input[is='coral-textfield']", pageSelector.segmentPath)
            //add a brand
            .click("button:contains('Add Brand')")
            .click(".groupedServices-ServiceSelector-service-title")
            // add an area reference
            .click("coral-select[name='./cq:target-ambits'] button")
            .click("coral-selectlist-item:contains('Master Area')")
            /*****  Check if the date is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{before:2000,expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Personalization')", {after: 1000})

            //check the contextHub path
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:contextHubPath']").val() === contextHubPath
            })
            //check the segments path
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:contextHubSegmentsPath']").val() === pageSelector.segmentPath
            })
            //check the brand
            .assert.exist("section.coral-Form-fieldset h4:contains('We.Retail')");
    };

    /**
     * Test: Check the Add Permissions options of a page properties.
     */
    page.tcAddPermissionsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Add permissions for a page", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Permissions')", {after: 1000})
            //check if the "Permissions" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Permissions')").size() === 1
            })

            .click("button:contains('Add Permissions')")

            //add permissions for a user
            .fillInput("foundation-autocomplete.js-cq-sites-CreatePermissionsDialog-authorizableList input[is='coral-textfield']", "We.Retail Editors", {after: 1000})
            //.wait(100)
            .click("foundation-autocomplete.js-cq-sites-CreatePermissionsDialog-authorizableList coral-overlay:contains('We.Retail Editors') button")
            //check if the tag for the user was added
            .assert.exist("foundation-autocomplete.js-cq-sites-CreatePermissionsDialog-authorizableList coral-tag:contains('We.Retail Editors')")
            //check if the Add button is disabled
            .assert.exist("coral-dialog:contains('Add Permissions') button:contains('Add')[disabled]")

            //add the delete permission
            .click("coral-dialog:contains('Add Permissions') input[name='delete']")
            //check if Browse, Edit and Delete page checkboxes are checked
            .assert.exist("coral-dialog:contains('Add Permissions') coral-checkbox[name='read'][checked]")
            .assert.exist("coral-dialog:contains('Add Permissions') coral-checkbox[name='modify'][checked]")
            .assert.exist("coral-dialog:contains('Add Permissions') coral-checkbox[name='delete'][checked]")
            //check if the Add button is enabled
            .assert.exist("coral-dialog:contains('Add Permissions') button:contains('Add')[disabled]", false)

            //add permission
            .click("coral-dialog:contains('Add Permissions') button:contains('Add')")

            //check if the permission was added to the list
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors')").size() === 1
            })
            //check if the permissions were set correctly
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') td:eq(1) coral-icon").size() === 1
            })
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') td:eq(2) coral-icon").size() === 1
            })
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') td:eq(3) coral-icon").size() === 1
            })

            //check if the permission was added to the Effective Permissions list
            .click("button:contains('Effective Permissions')")
            .assert.exist(".cq-siteadmin-admin-properties-effective-permissions:contains('We.Retail Editors')")
            .click("coral-dialog:contains('Effective Permissions') button[title='Close']")

            //edit a permission
            .click("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') button.js-cq-sites-PermissionsProperties-edit")
            //check if Browse, Edit and Delete page checkboxes are checked
            .assert.exist("coral-dialog:contains('Edit Permissions') coral-checkbox[name='read'][checked]")
            .assert.exist("coral-dialog:contains('Edit Permissions') coral-checkbox[name='modify'][checked]")
            .assert.exist("coral-dialog:contains('Edit Permissions') coral-checkbox[name='delete'][checked]")
            //add the publish/unpublish permission
            .click("coral-dialog:contains('Edit Permissions') input[name='replicate']")
            //save the changes
            .click("coral-dialog:contains('Edit Permissions') button.js-cq-sites-EditPermissionsDialog-update")
            //check if the permission was added
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') td:eq(4) coral-icon").size() === 1
            })

            //delete permission from the list
            .click("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors') button.js-cq-sites-PermissionsProperties-delete")
            .click("button:contains('Delete')")
            .assert.exist("table.js-cq-sites-UserGroup-permissions:contains('We.Retail Editors')", false)
            ;
    };

    /**
     * Test: Check the Edit Closed User Group options of a page properties.
     */
    page.tcEditUserGroupPermissionsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Edit user group's permissions for a page", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Permissions')", {after: 1000})
            //check if the "Permissions" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Permissions')").size() === 1
            })

            .click("button:contains('Edit Closed User Group')",{after:1000})

            .fillInput("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList input[is='coral-textfield']", "We.Retail Editors", {after: 1000})
            //.wait()
            .click("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList coral-overlay:contains('We.Retail Editors') button")
            //check if the tag for the user was added
            .assert.exist("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList coral-tag:contains('We.Retail Editors')")
            .click("coral-dialog:contains('Edit Closed') button[title='Remove']")

            //add permissions for a user
            .fillInput("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList input[is='coral-textfield']", "We.Retail Editors", {after: 2000})
            //.wait()
            .click("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList coral-overlay:contains('We.Retail Editors') button")
            //check if the tag for the user was added
            .assert.exist("foundation-autocomplete.js-cq-sites-CUGPermissionsDialog-authorizableList coral-tag:contains('We.Retail Editors')")

            .click("coral-dialog:contains('Edit Closed') button:contains('Save')")

            //check if the permission was added to the list
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-ClosedUserGroup-permissions:contains('We.Retail Editors')").size() === 1
            })
            //check if the permissions were set correctly
            .assert.isTrue(function () {
                return h.find("table.js-cq-sites-ClosedUserGroup-permissions:contains('We.Retail Editors') td:eq(1) coral-icon").size() === 1
            })

            //delete permission from the list
            .click("table.js-cq-sites-ClosedUserGroup-permissions:contains('We.Retail Editors') button.js-cq-sites-ClosedUserGroup-delete")
            .click("button:contains('Delete')")
            .assert.exist("table.js-cq-sites-ClosedUserGroup-permissions:contains('We.Retail Editors')", false)
            ;
    };

    /**
     * Test: Check the Effective Permissions options of a page properties.
     */
    page.tcEffectivePermissionsPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Effective permissions for a page", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Permissions')", {after: 1000})
            //check if the "Permissions" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Permissions')").size() === 1
            })

            //open the effective permissions option
            .click("button:contains('Effective Permissions')")
            .click("coral-dialog:contains('Effective Permissions') button[title='Close']")
            ;
    };

    /**
     * Test: Check the Live Copy options of a page properties.
     */
    page.tcLiveCopyPageProperties = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Live Copy for a page", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        // create the live copy page, store page path in 'testLiveCopyPagePath'
            .execFct(function (opts, done) {
                c.createLiveCopy(h.param("testPagePath")(opts), c.rootPage, 'page_' + Date.now(), 'page_' + Date.now(), "testLiveCopyPagePath", done)
            })

            // open the new page in the sites
            .navigateTo("/sites.html%testLiveCopyPagePath%")

            //select the page
            .execFct(function (opts, done) {
                c.setPageName(h.param("testLiveCopyPagePath")(opts), "testPageName", done);
            })
            .navigateTo("/mnt/overlay/wcm/core/content/sites/properties.html?item=%testLiveCopyPagePath%")
            //.click('coral-columnview-item:contains("%testPageName%") coral-columnview-item-thumbnail')
            //.click("button.cq-siteadmin-admin-actions-properties-activator")

            .click("coral-tab-label:contains('Live Copy')", {after: 1000})
            //check if the "Live Copy" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Live Copy')").size() === 1
            })

            //check the Synchronize button
            .click("coral-actionbar-item:contains('Synchronize') button")
            .click(".coral3-Button--primary:contains('Sync')")

            //check the Reset button
            .click("coral-tab-label:contains('Live Copy')", {after: 1000})
            .click("coral-actionbar-item:contains('Reset') button")
            .click(".coral3-Button--warning:contains('Reset')")

            //check the Suspend button
            .click("coral-tab-label:contains('Live Copy')", {after: 1000})
            .click("coral-actionbar-item:contains('Suspend') button")
            .click(function () {
                return h.find("coral-anchorlist coral-list-item-content").eq(0)
            })
            .click(".coral3-Button--warning:contains('Suspend')",{after:1000})

            //check the Resume button
            .click("coral-tab-label:contains('Live Copy')", {after: 2000})
            .click("coral-actionbar-item:contains('Resume') button", {after: 5000})
            .click(".coral3-Button--warning:contains('Resume')", {after: 1000})

            .click("coral-tab-label:contains('Live Copy')", {after: 1000})
            .click("coral-actionbar-item:contains('Suspend') button")
            .click(function () {
                return h.find("coral-anchorlist coral-list-item-content").eq(1)
            })
            .click(".coral3-Button--warning:contains('Suspend')")

            //check the Detach button
            .click("coral-tab-label:contains('Live Copy')", {after: 1000})
            .click("coral-actionbar-item:contains('Detach') button")
            .click(".coral3-Button--warning:contains('Detach')")

            .wait(1000)
            // delete the test page we created for live copy
            .execFct(function (opts, done) {
                c.deletePage(h.param("testLiveCopyPagePath")(opts), done);
            })
            ;
    };

    /**
     * Test: Check the Advanced Configuration option of a page properties.
     */
    page.tcAdvancedConfigurationPageProperties = function(tcExecuteBeforeTest,tcExecuteAfterTest) {
        return new h.TestCase("Advanced Configuration page property", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest,
            metadata: {
                ignoreOn63: true
            }
        })
        // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            /***** Insert information for 'Settings' *****/

            //open the Advanced tab
            .click("coral-tab-label:contains('Advanced')", {after: 1000})
            //check if the "Advanced" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Advanced')").size() === 1
            })

            //test the configuration settings

            //set the configuration
            .fillInput("foundation-autocomplete[name='./cq:conf'] input[is='coral-textfield']", configuration, {after: 2000})
            .click("button[value='" + configuration + "']",{after:2000})

            /*****  Check if the configuration is saved *****/

            //save the configuration and open again the page property
            .click("coral-buttongroup button:contains('Save & Close')",{expectNav:true})
            .execTestCase(page.openPageProperties)
            .click("coral-tab-label:contains('Advanced')", {after: 2000})

            //check the configuration
            .assert.isTrue(function (opts) {
                return h.find("input[name='./cq:conf'] span:contains('" + configuration + "')")
            });
    };

    /**
     * Test: Check the Blueprint options of a page properties.
     */
    page.tcBlueprintPageProperties64 = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Blueprint for a page (6.4)", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest,
            metadata: {
                ignoreOn63: true
            }
        })

            // open the new page in the sites
            .navigateTo("/sites.html%testPagePath%")

            .execTestCase(page.openPageProperties)

            .click("coral-tab-label:contains('Blueprint')", {after: 1000})
            //check if the "Blueprint" option was selected
            .assert.isTrue(function () {
                return h.find("coral-tab.is-selected coral-tab-label:contains('Blueprint')").size() === 1
            })

            .click("coral-anchorbutton-label:contains('Rollout')",{after:2000, expectNav:true})
            //check if the page is selected
            .assert.isTrue(function () {
                return h.find("coral-checkbox.select-rollout[checked]").size() === 3
            })
            //check the Rollout page and all sub pages
            .click("coral-checkbox.coral-Form-field")
            //save the configuration
            .click(".cq-dialog-actions .cq-dialog-submit",{expectNav:true})
    };

    /**
     * Test: That header of We.Retail was loaded and added via Ajax.
     * @param tcExecuteBeforeTest
     * @param tcExecuteAfterTest
     * @returns {*}
     */
    page.tcLoadHeader = function (tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Include Header via Ajax on publish", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest,
            metadata: {
                ignoreOn63: true
            }
        })
        // open the new page in wcmmode disabled
            .navigateTo("%testPagePath%.html?wcmmode=disabled")

            .assert.isTrue(function () {
                $(document).on("we-header-content", function () {
                    h.find(".navbar").size() === 2;
                });
            })
    };

    var tcExecuteBeforeTest = page.tcExecuteBeforeTest();
    var tcExecuteAfterTest = page.tcExecuteAfterTest();

    /**
     * The main test suite for Page component
     */
    new h.TestSuite("We.Retail Tests - Page", {path:"/apps/weretail/tests/admin/components-it/Page/Page.js",
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(page.tcBasicTitleAndTagsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcBasicTitlesAndDescriptionsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcBasicOnOffTimePageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcBasicVanityUrlPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAdvancedSettingsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAdvancedTemplatesSettingsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAdvancedConfigurationPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAdvancedAuthenticationPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAdvancedExportPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcThumbnailPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcSocialMediaPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcCloudServicesPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcPersonalizationPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcAddPermissionsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcEditUserGroupPermissionsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcEffectivePermissionsPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcBlueprintPageProperties64(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcLiveCopyPageProperties(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(page.tcLoadHeader(tcExecuteBeforeTest, tcExecuteAfterTest))
    ;

}(hobs, jQuery));

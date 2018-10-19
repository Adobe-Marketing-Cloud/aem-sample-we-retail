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

;(function(h, $) {

    // don't remove this setting
    hobs.config.pacing_delay = 1;

    // shortcut
    var c = window.CQ.WeRetailIT.commons;

    // the root page defined in the test content package
    c.rootPage = "/content/we-retail/language-masters/en";
    // the template defined in the test content package
    c.template = "/conf/we-retail/settings/wcm/templates/hero-page";
    // relative path from page node to the root layout container
    c.relParentCompPath = "/jcr:content/root/responsivegrid/";
    // breadcrumb component
    c.rtBreadcrumb = "weretail/components/content/breadcrumb";
    // carousel component
    c.rtCarousel = "weretail/components/content/carousel";
    // image component
    c.rtImage = "weretail/components/content/image";
    // hero image component
    c.rtHeroImage = "weretail/components/content/heroimage";
    // category teaser component
    c.rtTeaser = "weretail/components/content/teaser";
    //title component
    c.rtTitle = "weretail/components/content/title";
    //text component
    c.rtText = "weretail/components/content/text";
    //list component
    c.rtList = "weretail/components/content/list";
    //form button
    c.rtFormButton = "weretail/components/form/button";
    //form hidden
    c.rtFormHidden = "weretail/components/form/hidden";
    //form option
    c.rtFormOptions = "weretail/components/form/options";
    //form text
    c.rtFormText = "weretail/components/form/text";
    // form container
    c.rtFormContainer = "weretail/components/form/container";


    // the path to the policies
    c.policyPath = "/conf/we-retail/settings/wcm/policies/weretail/components/content";
    // the policy assignment path
    c.policyAssignmentPath = "/conf/we-retail/settings/wcm/templates/hero-page/policies/jcr:content/root/responsivegrid/weretail/components/content";

    // configuration dialog
    c.selConfigDialog = ".cq-dialog.foundation-form.foundation-layout-form";
    // save button on a configuration dialog
    c.selSaveConfDialogButton = ".cq-dialog-actions button[is='coral-button'][title='Done']";

    /**
     * Creates a CQ page via POST request, the same as send by the create page wizard.
     *
     * @param templatePath Mandatory. Path to the template e.g. "/conf/coretest/settings/wcm/templates/content-page"
     * @param parentPath Mandatory. Path to the parent page e.g. "/content/coretest/language-masters/en"
     * @param pageName Mandatory. Page name to be set for the page.
     * @param dynParName Optional. Hobbes dynamic param to store the generated page path.
     * @param done Mandatory. Callback to be executed when async method has finished.
     * @param [testPageRT='core/wcm/tests/components/test-page'] the resource type of the test page
     */
    c.createPage = function (templatePath, parentPath, pageName, dynParName, done, testPageRT) {
        // mandatory check
        if (parentPath == null || templatePath == null || pageName == null || done == null) {
            if (done) done(false, "createPage failed! mandatory parameter(s) missing!");
            return;
        }

        // the ajax call
        jQuery.ajax({
            url: "/libs/wcm/core/content/sites/createpagewizard/_jcr_content",
            method: "POST",
            // POST data to be send in the request
            data: {
                "template": templatePath,
                "parentPath": parentPath,
                "_charset_": "utf-8",
                "./jcr:title": pageName,
                "pageName": pageName,
                "./sling:resourceType": testPageRT || "weretail/components/structure/page"
            }
        })
        // when the request was successful
            .done(function (data, textStatus, jqXHR) {
                // extract the created page path from the returned HTML
                var path = jQuery(data).find("#Path").text();
                // get the page name
                var name = path.substring(path.lastIndexOf("/") + 1, path.length);
                // if the page already existed it will stupidly postfix it with a number this can lead to problems
                // so at least we should log a warning
                if (pageName != name) {
                    done(false, "createPage failed! page was created with different name!");
                }
                // store the page path and name as dynamic data for reuse in hobs functions
                if (dynParName != null) {
                    hobs.param(dynParName, path);
                }
            })
            // request fails
            .fail(function (jqXHR, textStatus, errorThrown) {
                // log an error
                done(false, "createPage failed! POST failed with: " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    /**
     * Deletes a page.
     *
     * @param pagePath Mandatory. testPagePath path to the page to be deleted
     * @param done Optional. callback to be executed when the async method has finished.
     */
    c.deletePage = function (pagePath, done) {
        // mandatory check
        if (pagePath == null || done == null) {
            if (done) done(false, "deletePage failed! mandatory parameter(s) missing!");
            return;
        }
        jQuery.ajax({
            url: pagePath,
            method: "POST",
            data: {
                ":operation": "delete"
            }
        })
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "deletePage failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    c.addComponent = function (component, parentCompPath, dynParName, done, nameHint, order) {
        // mandatory check
        if (component == null || parentCompPath == null || done == null) {
            if (done) done(false, "addComponent failed! mandatory parameter(s) missing!");
            return;
        }

        // default settings
        if (nameHint == null) nameHint = component.substring(component.lastIndexOf("/") + 1);
        if (order == null) order = "first";

        // the ajax call
        jQuery.ajax({
            url: parentCompPath,
            method: "POST",
            data: {
                "./sling:resourceType": component,
                "./jcr:created": "",
                "./jcr:lastModified": "",
                "./cq:lastModified": "",
                ":order": order,
                "_charset_": "utf-8",
                ":nameHint": nameHint

            }
        })
            .done(function (data, textStatus, jqXHR) {
                // extract the component path from the returned HTML
                if (dynParName != null) {
                    h.param(dynParName, jQuery(data).find("#Path").text());
                }
            })
            // in case of failure
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "addComponent failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    c.deleteComponent = function (component_path, done) {
        // mandatory check
        if (component_path == null || done == null) {
            if (done) done(false, "deleteComponent failed! mandatory parameter(s) missing!");
            return;
        }

        // the ajax call
        jQuery.ajax({
            url: component_path,
            method: "POST",
            data: {
                ":operation": "delete"
            }
        })
        .fail(function (jqXHR, textStatus, errorThrown) {
            done(false, "deletePolicy failed: POST failed with " + textStatus + "," + errorThrown);
        })
        // always executed, fail or success
        .then(function () {
            done(true);
        })
    };

    /**
     * returns the content frame.
     */
    c.getContentFrame = function () {
        return h.find('iframe#ContentFrame').get(0);
    };

    c.closeSidePanel = new hobs.TestCase("Close side panel", {timeout: 2000})
        .ifElse(function (opts) {
                var clickToggle = hobs.find('.toggle-sidepanel.editor-GlobalBar-item').length > 0 &&
                    hobs.find('#SidePanel.sidepanel-opened').length > 0 &&
                    hobs.find('.toggle-sidepanel.editor-GlobalBar-item').length > 0;
                return clickToggle;
            },
            click('.toggle-sidepanel.editor-GlobalBar-item')
        );

    c.disableTutorials = new hobs.TestCase("Disable Tutorials (preferences)")
        .execFct(function (opts, done) {
                // set language to config locale value
                var result = Granite.HTTP.eval("/libs/granite/csrf/token.json");
                var user = Granite.HTTP.eval(hobs.config.context_path + "/libs/cq/security/userinfo.json");
                var data = {
                    "cq.authoring.editor.page.showTour62": false,
                    "cq.authoring.editor.page.showOnboarding62": false,
                    "cq.authoring.editor.template.showTour": false,
                    "cq.authoring.editor.template.showOnboarding": false,
                    "granite.shell.showonboarding620": false
                };
                data[':cq_csrf_token'] = result.token;
                jQuery.post(hobs.config.context_path + user.home + "/preferences", data)
                    .always(function () {
                        done();
                    });
            }
    );

    c.openSidePanel = function (done) {

        maxRetries = 5;
        timeout = 500;

        // retry counter
        var retries = 0;

        // the polling function
        var poll = function () {
            // if the panel is closed
            if (h.find("#SidePanel.sidepanel-closed").size() == 1) {
                // click on the toggle button, wait for the click to finish, then check
                click(".toggle-sidepanel.editor-GlobalBar-item").exec().then(
                    function () {
                        // check if the panel is open
                        if (h.find("#SidePanel.sidepanel-opened").size() == 1) {
                            done(true);
                        } else {
                            // check if max retries was reached
                            if (retries++ === maxRetries) {
                                done(false, "Opening the Side Panel failed!");
                                return;
                            }
                            // set for next retry
                            setTimeout(poll, timeout);
                        }
                    }
                )
            }
            done();

        };
        // start polling
        poll();
    };

    /**
     * Requests the JSON output and stores it in a hobbes dynamic parameter.
     * Does several retries and works async. so callback method 'done' should be set to avoid problems.
     *
     * @param url           mandatory. the JSON Url to request.
     * @param dynParName    mandatory. the hobbes param to store the JSON object in.
     * @param done          mandatory. the callback to execute when finished
     * @param maxRetries    optional. number of retries, default 10
     * @param timeout       optional. timout in milliseconds between retries, default 500
     */
    c.getJSON = function (url, dynParName, done, maxRetries, timeout) {
        // check mandatory
        if (url == null || dynParName == null || done == null) {
            if (done) done(false, "getJSON failed! Mandatory param(s) missing.");
            return;
        }

        // check defaults
        if (maxRetries == null) maxRetries = 10;
        if (timeout == null) timeout = 500;

        // retry counter
        var retries = 0;

        // the polling function
        var poll = function () {
            $.ajax({
                url: url,
                method: "GET",
                dataType: "json"
            })
                .done(function (data) {
                    h.param(dynParName, data);
                    done(true);
                })
                .fail(function (jqXHR, textStatus, errorThrown) {
                    // check if max retries was reached
                    if (retries++ === maxRetries) {
                        done(false, "getJSON failed! GET failed with " + textStatus + "," + errorThrown);
                        return;
                    }
                    // set for next retry
                    setTimeout(poll, timeout);
                })
        };
        // start polling
        poll();
    };

    /**
     * returns the page name.
     */
    c.setPageName = function (pagePath, pageName, done) {
        // mandatory check
        if (pagePath == null || pageName == null || done == null) {
            if (done) done(false, "setPageName failed! mandatory parameter(s) missing!");
            return;
        }
        var name = pagePath.substring(pagePath.lastIndexOf("/") + 1, pagePath.length);
        hobs.param(pageName,name);

        done(true);
    };

    /**
     * Opens the inline editor for a component. Uses 'data-path' attribute to identify the correct
     * component.
     *
     * @param cmpPath   mandatory. the absolute component path, used as the value for 'data-path' attribue
     */
    c.tcOpenInlineEditor = function(cmpPath) {
        return new TestCase("Open Inline Editor")
        //click on the component to see the Editable Toolbar
            .click(".cq-Overlay.cq-draggable.cq-droptarget%dataPath%", {
                before: function () {
                    // set the data-path attribute so we target the correct component
                    h.param("dataPath", "[data-path='" + h.param(cmpPath)() + "']");
                }
            })

            // check if its there
            .asserts.visible("#EditableToolbar")
            // click on the 'Edit' button
            .click("button.cq-editable-action[is='coral-button'][title='Edit']")
            // go to the content frame
            .config.changeContext(c.getContentFrame)
            // wait for editable attribute to appear
            .assert.exist("[contenteditable='true']")
            // go back to previous context
            .config.resetContext();
    };

    /**
     * Closes any previously opened inline editor by clicking on the save button
     */
    c.tcSaveInlineEditor = new TestCase("Save Inline Editor")
    //click on the component to see the Editable Toolbar
        .click("button[is='coral-button'][title='Save']");

    /**
     * Sets properties of a repository node.
     *
     * @param componentPath     Mandatory. absolute path to the node
     * @param data              Mandatory. object with properties to be set on the node.
     * @param done              Mandatory. callback function when post has returned
     */
    c.editNodeProperties = function (componentPath, data, done) {
        // check mandatory
        if (componentPath == null || data == null || done == null) {
            if (done) done(false, "editNodeProperties failed! Mandatory param(s) missing.");
            return;
        }
        $.ajax({
            url: componentPath,
            method: "POST",
            // POST data to be send in the request
            data: data
        })
        // in case of failure
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "editNodeProperties failed: POST failed with " + textStatus + "," + errorThrown);
            })
            .then(function () {
                done(true);
            })
    };

    c.tcUseDialogSelect = function(name,value){
        return new TestCase("Set Select for '" + name + "' to '" + value + "'")
        // open action drop down
            .click("coral-select[name='" + name + "'] > button")
            // check if the dropdown has become visible
            .assert.visible("coral-select[name='" + name + "'] coral-selectlist")
            // select the store action
            .click("coral-select[name='" + name + "'] coral-selectlist " +
                "coral-selectlist-item[value='" + value + "']")
    };

    /**
     * Selects a value in a Granite UI autocomplete field
     *
     * @param selector {String} Specific selector for the autocomplete (ex. "[name='./myField']")
     * @param value {String} The value to be selected
     *
     * @returns {TestCase} A test case that selects a value in an autocomplete field
     */
    c.tcSelectInAutocomplete = function(selector, value) {
        return new TestCase("Select in autocomplete the value '" + value + "' for " + selector)
            // fill input
            .fillInput("foundation-autocomplete" + selector + " input[type!='hidden']", value)
            // click on the item in the open suggestion box
            .click("foundation-autocomplete" + selector + " button[value^='" + value + "']", {after: 1000})
    };

    /**
     * Selects a tag in a Granite UI tags autocomplete field
     *
     * @param selector {String} Specific selector for the tags selector (ex. "[name='./tags']")
     * @param value {String} The tag value
     * @returns {TestCase} A test case that selects a tag in a tag selector field
     */
    c.tcSelectInTags = function(selector, value) {
        return new TestCase("Select in autocomplete the value '" + value + "' for '" + selector)
            // fill input
            .fillInput("foundation-autocomplete" + selector + " input[is='coral-textfield']",  value)
            // simulate Enter key
            .simulate("foundation-autocomplete" + selector + " input[is='coral-textfield']", "keydown", {keyCode: 13, after: 1000})
    };

    /**
     * Adds a tag in default namespace
     *
     * @param tag   Mandatory. the tag to be added
     * @param done  Mandatory. the callback to execute when post returns
     */
    c.addTag = function (tag, done) {
        // mandatory check
        if (tag == null || done == null) {
            if (done) done(false, "addTag failed! Mandatory param(s) missing.");
            return;
        }
        jQuery.ajax({
            url: "/bin/tagcommand",
            method: "POST",
            data: {
                "cmd": "createTagByTitle",
                "tag": tag,
                "locale": "en",
                "_charset_": "utf-8"

            }
        })
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "addTag failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    /**
    * Common stuff that should be done before each test case starts.
    */
    c.tcExecuteBeforeTest = new TestCase("Common Set up")
        // reset the context
        .config.resetContext()

        .navigateTo("/content/we-retail/language-masters/en.html")
        .execFct(function(opts) {
            // make sure we start in edit mode
            $.cookie('cq-editor-layer.page', 'Edit', { path : "/" });
            // make sure the  side panel starts  closed
            $.cookie('cq-editor-sidepanel', 'closed', { path : "/" });
        })
        // editing cookies requires a reload
        .reloadPage();

    /**
    * Common stuff that should be done at the end of each test case.
     */
    c.tcExecuteAfterTest = new TestCase("Common Clean Up")
    // reset the context
        .config.resetContext()
        // make sure the side panel is closed
        .execTestCase(c.closeSidePanel);

    /**
    * Stuff that should be done before a testsuite starts
     */
    c.tcExecuteBeforeTestSuite =  new TestCase("Setup Before Testsuite")
    // disable tutorial popups
        .execTestCase(c.disableTutorials);

    /**
     * Opens the configuration dialog for a component. Uses 'data-path' attribute to identify the correct
     * component.
     *
     * @param cmpPath   mandatory. the absolute component path, used as the value for 'data-path' attribue
     */
    c.tcOpenConfigureDialog = function(cmpPath) {
        return new TestCase("Open Configure Dialog")
        //click on the component to see the Editable Toolbar
            .click(".cq-Overlay.cq-draggable.cq-droptarget%dataPath%",{
                before: function() {
                    // set the data-path attribute so we target the correct component
                    h.param("dataPath", "[data-path='" + h.param(cmpPath)() + "']");
                }
            })

            // make sure its visible
            .asserts.visible("#EditableToolbar")
            // click on the 'configure' button
            .click("button.cq-editable-action[is='coral-button'][title='Configure']")
            // verify the dialog has become visible
            .asserts.visible(c.selConfigDialog);
    };

    /**
     * Closes any open configuration dialog
     */
    c.tcSaveConfigureDialog = new TestCase ("Save Configure Dialog")
    // if full Screen mode was used make sure the click waits for the navigation change
        .ifElse(
            // check if the dialog opened in a different URL
            function(){ return hobs.context().window.location.pathname.startsWith("/mnt/override")}
            ,
            TestCase("Close Fullscreen Dialog")
            // NOTE: this will cause test to fail if the dialog can't be closed e.g. due to missing mandatory values
                .click(c.selSaveConfDialogButton,{expectNav:true})
            ,
            TestCase("Close Modal Dialog")
                .click(c.selSaveConfDialogButton,{expectNav:false})
            ,{ timeout:10 });

    /**
     * Helper test case: switch the config dialog tab
     */
    c.tcSwitchConfigTab = function(tabTitle){
        return new TestCase("Switch Config Tab to " +  tabTitle)
        // click on the tab
            .click("coral-tab > coral-tab-label:contains('"+ tabTitle + "')")
            // check if its selected
            .assert.isTrue(function(){
                return h.find("coral-tab[selected] > coral-tab-label:contains('"+ tabTitle + "')").size() == 1
            })
    };

    /**
     * Creates a Live Copy via POST request.
     *
     * @param srcPath Mandatory. Path to the source page for the live copy."
     * @param destPath Mandatory. Path to the destination page for the live copy."
     * @param title Mandatory. the title to be set for the live copy.
     * @param label Mandatory. The label to be set for the live copy.
     * @param dynParName Optional. Hobbes dynamic param to store the generated page path.
     * @param done Mandatory. Callback to be executed when async method has finished.
     */
    c.createLiveCopy = function (srcPath, destPath, title, label, dynParName, done) {
        // mandatory check
        if (srcPath == null || destPath == null || title == null || label == null || done == null) {
            if (done) done(false, "createLiveCopy failed! mandatory parameter(s) missing!");
            return;
        }

        // the ajax call
        jQuery.ajax({
            url: "/bin/wcmcommand",
            method: "POST",
            // POST data to be send in the request
            data: {
                "cmd": "createLiveCopy",
                "srcPath": srcPath,
                "destPath": destPath,
                "_charset_": "utf-8",
                "title": title,
                "label": label
            }
        })
        // when the request was successful
            .done(function (data, textStatus, jqXHR) {
                // extract the created page path from the returned HTML
                var path = jQuery(data).find("#Path").text();

                // store the page path and name as dynamic data for reuse in hobs functions
                if (dynParName != null) {
                    hobs.param(dynParName, path);
                }
            })

            // request fails
            .fail(function (jqXHR, textStatus, errorThrown) {
                // log an error
                done(false, "createLiveCopy failed! POST failed with: " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };


    /**
     * Create a policy
     *
     * @param component_path   Mandatory. the path to the component policy
     * @param data Tha policy's data
     * @param done  Mandatory. the callback to execute when post returns
     */
    c.createPolicy = function (component_path, data, dynParName, done, policyPath) {
        policyPath = policyPath || c.policyPath;
        // mandatory check
        if (component_path == null || data == null || done == null) {
            if (done) done(false, "createPolicy failed! Mandatory param(s) missing.");
            return;
        }
        jQuery.ajax({
            url: policyPath + component_path,
            method: "POST",
            data: data
        })
            .done(function (data, textStatus, jqXHR) {
                // extract the component path from the returned HTML
                if (dynParName != null) {
                    h.param(dynParName, jQuery(data).find("#Path").text());
                }
            })

            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "createPolicy failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    /**
     * Assign a policy to a core component
     *
     * @param component_path   Mandatory. the path to the component policy
     * @param data Tha policy's data
     * @param done  Mandatory. the callback to execute when post returns
     */
    c.assignPolicy = function (component_path, data, done, policyAssignmentPath) {
        policyAssignmentPath = policyAssignmentPath || c.policyAssignmentPath;
        // mandatory check
        if (component_path == null || data == null || done == null) {
            if (done) done(false, "assignPolicy failed! Mandatory param(s) missing.");
            return;
        }
        jQuery.ajax({
            url: policyAssignmentPath + component_path,
            method: "POST",
            data: data
        })
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "assignPolicy failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    /**
     * Deletes a policy.
     *
     * @param policyPath Mandatory. policyPath path to the policy to be deleted
     * @param done Optional. callback to be executed when the async method has finished.
     */
    c.deletePolicy = function (component_path, done, policyPath) {
        policyPath = policyPath || c.policyPath;
        // mandatory check
        if (component_path == null || done == null) {
            if (done) done(false, "deletePolicy failed! mandatory parameter(s) missing!");
            return;
        }
        jQuery.ajax({
            url: policyPath + component_path,
            method: "POST",
            data: {
                ":operation": "delete"
            }
        })
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "deletePolicy failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

    /**
     * Deletes a policy allocation.
     *
     * @param policyAllocationPath Mandatory. policyAllocatiionPath path to the policy allocation to be deleted
     * @param done Optional. callback to be executed when the async method has finished.
     */
    c.deletePolicyAssignment = function (component_path, done, policyAssignmentPath) {
        policyAssignmentPath = policyAssignmentPath || c.policyAssignmentPath;
        // mandatory check
        if (component_path == null || done == null) {
            if (done) done(false, "deletePolicyAllocation failed! mandatory parameter(s) missing!");
            return;
        }
        jQuery.ajax({
            url: policyAssignmentPath + component_path,
            method: "POST",
            data: {
                ":operation": "delete"
            }
        })
            .fail(function (jqXHR, textStatus, errorThrown) {
                done(false, "deletePolicyAllocation failed: POST failed with " + textStatus + "," + errorThrown);
            })
            // always executed, fail or success
            .then(function () {
                done(true);
            })
    };

}(hobs, jQuery));

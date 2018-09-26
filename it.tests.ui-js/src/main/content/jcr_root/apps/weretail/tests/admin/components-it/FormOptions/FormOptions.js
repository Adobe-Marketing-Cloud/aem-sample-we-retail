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

    // shortcut
    var c = window.CQ.WeRetailIT.commons;
    var formOptions = window.CQ.WeRetailIT.FormOptions;

    //element name
    var elemName = "form_options";
    //title value
    var title = "Options";
    //help message
    var helpMessage = "This is an help message"
    //value for 'value' field
    var value = "value1"
    //value for 'text' field
    var text = "text1"

    var itemSelector ={
        help: '.cmp-form-options__help-message',
        checkbox: '.cmp-form-options__field--checkbox',
        radio: '.cmp-form-options__field--radio',
        dropDown: '.cmp-form-options__field--drop-down',
        multiDropDown: '.cmp-form-options__field--multi-drop-down'
    };

    /**
     * Before Test Case
     */
    formOptions.tcExecuteBeforeTest = function() {
        return new TestCase("Setup Before Test")
        //common set up
            .execTestCase(c.tcExecuteBeforeTest)
            //create the test page, store page path in 'testPagePath'
            .execFct(function (opts, done) {
                c.createPage(c.template, c.rootPage, 'page_' + Date.now(), "testPagePath", done)
            })
            //add the component, store component path in 'optionPath'
            .execFct(function (opts, done) {
                c.addComponent(c.rtFormOptions, h.param("testPagePath")(opts) + c.relParentCompPath, "optionPath", done)
            })
            //open the new page in the editor
            .navigateTo("/editor.html%testPagePath%.html");
    };

    /**
     * After Test Case
     */
    formOptions.tcExecuteAfterTest = function() {
        return new TestCase("Clean up after Test")
        //common clean up
            .execTestCase(c.tcExecuteAfterTest)
            //delete the test page we created
            .execFct(function (opts, done) {
                c.deletePage(h.param("testPagePath")(opts), done);
            });
    };

    /**
     * Helper test case: set the mandatory fields
     */
    formOptions.setMandatoryFields = new h.TestCase("Set Mandatory Fields")
    //set the mandatory title text
        .fillInput("[name='./jcr:title']",title)
        //set the mandatory element name
        .fillInput("[name='./name']",elemName)
    ;

    /**
     * Helper test case: add an option
     */
    formOptions.addOption = new h.TestCase("Add one option to the Form Options")
    //press the Add button
        .click("button :contains('Add')")
        //set the value
        .fillInput("input[name$='value']",value)
        //set the text
        .fillInput("input[name$='text']",text)
    ;

    /**
     * Helper function: set the option type
     */
    formOptions.setOptionType = function(optionType) {
        return new h.TestCase("Set Form Input Type to " + optionType)
        //open the dropdown
            .click("coral-select[name='./type'] button")
            //wait for the dropdown to appear
            .assert.visible("coral-select[name='./type'] coral-selectlist")
            //select the option type
            .click("coral-select[name='./type'] coral-selectlist-item[value='" + optionType + "']");
    };

    /**
     * Test: Check the mandatory fields
     */
    formOptions.checkMandatoryFields = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Check Mandatory fields", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            .click(c.selSaveConfDialogButton, {expectNav: false})
            //check if the dialog is still open
            .asserts.visible(c.selConfigDialog)
            //check if label marked as invalid
            .asserts.isTrue(function () {
                return h.find("input[name='./jcr:title'].is-invalid").size() == 1
            })
            //check if element name is marked as invalid
            .asserts.isTrue(function () {
                return h.find("input[name='./name'].is-invalid").size() == 1
            });
    };

    /**
     * Test: Set title text
     */
    formOptions.setTitle = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set title", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the title is rendered
            .asserts.isTrue(function () {
                return h.find("legend", "#ContentFrame").text().trim() == title
            });
    };

    /**
     * Test: Set element name
     */
    formOptions.setElementName = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set Element Name", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if input name is set correctly
            .asserts.isTrue(function () {
                return h.find("input[name='" + elemName + "']", "#ContentFrame").size() == 1;
            });
    };

    /**
     * Test: Set the help message
     */
    formOptions.setHelpMessage = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set Help Message", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //set the help message
            .fillInput("[name='./helpMessage']", helpMessage)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the help message is set correctly
            .asserts.isTrue(function () {
                return h.find(itemSelector.help, "#ContentFrame").text().trim() == helpMessage;
            });
    };

    /**
     * Test : Set the checkbox type
     */
    formOptions.setCheckbox = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set checkbox type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //set the option type to checkbox
            .execTestCase(formOptions.setOptionType("checkbox"))
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the option type is set to checkbox
            .asserts.isTrue(function () {
                return h.find(itemSelector.checkbox, "#ContentFrame").size() == 1;
            });
    };

    /**
     * Test : Set the radio button type
     */
    formOptions.setRadioButton = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set radio button type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //set the option type to radio button
            .execTestCase(formOptions.setOptionType("radio"))
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the option type is set to radio button
            .asserts.isTrue(function () {
                return h.find(itemSelector.radio, "#ContentFrame").size() == 1;
            });
    };

    /**
     * Test : Set the drop-down type
     */
    formOptions.setDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set drop-down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //set the option type to drop-down
            .execTestCase(formOptions.setOptionType("drop-down"))
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the option type is set to drop-down
            .asserts.isTrue(function () {
                return h.find(itemSelector.dropDown, "#ContentFrame").size() == 1;
            });
    };

    /**
     * Test : Set the multi-select drop-down type
     */
    formOptions.setMultiSelectDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set multi-select drop-down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })

        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //set the option type to multi-select drop-down
            .execTestCase(formOptions.setOptionType("multi-drop-down"))
            //add one option
            .execTestCase(formOptions.addOption)
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)

            //check if the option type is set to multi-select drop-down
            .asserts.isTrue(function () {
                return h.find(itemSelector.multiDropDown, "#ContentFrame").size() == 1;
            });
    };

    formOptions.setActiveOptionForCheckbox = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Active' option for the Checkbox type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to checkbox
            .execTestCase(formOptions.setOptionType("checkbox"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Active' option
            .click("input[type='checkbox'][name$='selected']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is active
            .asserts.isTrue(function () {
                return h.find("input[type='checkbox'][value=" + value + "][checked]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setActiveOptionForRadioButton = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Active' option for the Radio button type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to radio
            .execTestCase(formOptions.setOptionType("radio"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Active' option
            .click("input[type='radio'][name$='selected']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is active
            .asserts.isTrue(function () {
                return h.find("input[type='radio'][value='" + value + "'][checked]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setActiveOptionForDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Active' option for the Drop down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to drop-down
            .execTestCase(formOptions.setOptionType("drop-down"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Active' option
            .click("input[type='radio'][name$='selected']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is active
            .asserts.isTrue(function () {
                return h.find("option[value='" + value + "'][selected]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setActiveOptionForMultiSelectDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Active' option for the Multi select drop down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to multi-select drop-down
            .execTestCase(formOptions.setOptionType("multi-drop-down"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Active' option
            .click("input[type='checkbox'][name$='selected']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is active
            .asserts.isTrue(function () {
                return h.find("option[value='" + value + "'][selected]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setDisabledOptionForCheckbox = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Disabled' option for the Checkbox type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to checkbox
            .execTestCase(formOptions.setOptionType("checkbox"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Disabled' option
            .click("input[type='checkbox'][name$='disabled']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is disabled
            .asserts.isTrue(function () {
                return h.find("input[type='checkbox'][value=" + value + "][disabled]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setDisabedOptionForRadioButton = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Disabled' option for the Radio button type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to radio
            .execTestCase(formOptions.setOptionType("radio"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Disabled' option
            .click("input[type='checkbox'][name$='disabled']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is disabled
            .asserts.isTrue(function () {
                return h.find("input[type='radio'][value='" + value + "'][disabled]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setDisabledOptionForDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Disabled' option for the Drop down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to drop-down
            .execTestCase(formOptions.setOptionType("drop-down"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Disabled' option
            .click("input[type='checkbox'][name$='disabled']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is disabled
            .asserts.isTrue(function () {
                return h.find("option[value='" + value + "'][disabled]", "#ContentFrame").size() == 1;
            });
    };

    formOptions.setDisabledOptionForMultiSelectDropDown = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
        return new h.TestCase("Set the 'Disabled' option for the Multi select drop down type", {
            execBefore: tcExecuteBeforeTest,
            execAfter: tcExecuteAfterTest
        })
        //open the edit dialog
            .execTestCase(c.tcOpenConfigureDialog("optionPath"))
            //set the option type to multi-select drop-down
            .execTestCase(formOptions.setOptionType("multi-drop-down"))
            //set the mandatory fields
            .execTestCase(formOptions.setMandatoryFields)
            //add one option
            .execTestCase(formOptions.addOption)
            //check the 'Disabled' option
            .click("input[type='checkbox'][name$='disabled']")
            //close the edit dialog
            .execTestCase(c.tcSaveConfigureDialog)
            //check if the option is disabled
            .asserts.isTrue(function () {
                return h.find("option[value='" + value + "'][disabled]", "#ContentFrame").size() == 1;
            });
    };

    var tcExecuteBeforeTest = formOptions.tcExecuteBeforeTest();
    var tcExecuteAfterTest = formOptions.tcExecuteAfterTest();

    /**
     * The main test suite.
     */
    new h.TestSuite('We.Retail Tests - Form Options', {path: '/apps/weretail/tests/admin/components-it/FormOptions.js',
        execBefore:c.tcExecuteBeforeTestSuite,
        execInNewWindow : false})

        .addTestCase(formOptions.checkMandatoryFields(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setTitle(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setElementName(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setHelpMessage(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setCheckbox(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setRadioButton(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setMultiSelectDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setActiveOptionForCheckbox(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setActiveOptionForRadioButton(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setActiveOptionForDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setActiveOptionForMultiSelectDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setDisabledOptionForCheckbox(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setDisabedOptionForRadioButton(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setDisabledOptionForDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
        .addTestCase(formOptions.setDisabledOptionForMultiSelectDropDown(tcExecuteBeforeTest, tcExecuteAfterTest))
    ;
}(hobs, jQuery));
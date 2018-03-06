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

  var c = window.CQ.WeRetailIT.commons;
  var languageNavigation = window.CQ.WeRetailIT.LanguageNavigation;

  var alternativeLanguage = {
    code: 'es-US',
    pathToken: '/us/es.html',
    title: 'Español'
  };

  var selectors = {
    modal: {
      self: '.we-LanguageModal',
      open: '.we-retail-header .navbar-left [data-target=".we-LanguageModal"]',
      close: '.we-LanguageModal [data-dismiss="modal"]'
    },
    languageNavigation: {
      link: '.cmp-languagenavigation__item-link'
    },
    languages: {
      spanish: 'Español'
    },
    flags: {
      US: '.we-lang-icon-US'
    }
  };

  /**
   * Before Test Case
   *
   * 1. Open US market English language page in publish mode
   */
  languageNavigation.tcExecuteBeforeTest = function() {
    return new TestCase("Before Language Navigation test")
      .execTestCase(c.tcExecuteBeforeTest)

      // 1
      .navigateTo("/content/we-retail/us/en.html?wcmmode=disabled");
  };

  /**
   * After Test Case
   */
  languageNavigation.tcExecuteAfterTest = function() {
    return new TestCase("After Language Navigation test")
      .execTestCase(c.tcExecuteAfterTest);
  };

  /**
   * Test: Open and close language navigation modal.
   *
   * 1. Open modal
   * 2. Close modal
   */
  languageNavigation.tcExecuteOpenCloseModal = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
    return new h.TestCase('Open and close modal', {
      execBefore: tcExecuteBeforeTest,
      execAfter: tcExecuteAfterTest})

      // 1
      .click(selectors.modal.open)
      .asserts.exists(selectors.modal.self + ':visible')

      // 2
      .click(selectors.modal.close, { delayBefore: 1000 })
      .asserts.exists(selectors.modal.self + ':visible', false)
  };

  /**
   * Test: Switch language and verify location change.
   *
   * 1. Open modal
   * 2. Check flags are displayed
   * 3. Choose alternative language
   * 4. Verify expected location change
   * 5. Check that the open toggle button reflects the new language
   */
  languageNavigation.tcExecuteSwitchLanguage = function(tcExecuteBeforeTest, tcExecuteAfterTest) {
    return new h.TestCase('Switch language', {
      execBefore: tcExecuteBeforeTest,
      execAfter: tcExecuteAfterTest})

      // 1
      .click(selectors.modal.open)
      .asserts.exists(selectors.modal.self + ':visible')

      // 2
      .asserts.exists(selectors.modal.self + ' ' + selectors.flags.US + ':visible')

      // 3
      .click(selectors.languageNavigation.link + '[lang=' + alternativeLanguage.code + ']')

      // 4
      .asserts.isTrue(function() {
        return h.context().window.location.pathname.includes(alternativeLanguage.pathToken);
      }, { delayBefore: 5000 })

      // 5
      .asserts.exists(selectors.modal.open + '[title=' + alternativeLanguage.title + ']' + ':visible');
  };

  var tcExecuteBeforeTest = languageNavigation.tcExecuteBeforeTest();
  var tcExecuteAfterTest = languageNavigation.tcExecuteAfterTest();

  /**
   * The main test suite.
   */
  new h.TestSuite('We.Retail Tests - Language Navigation', {
    path: '/apps/weretail/tests/admin/components-it/LanguageNavigation/LanguageNavigation.js',
    execBefore: c.tcExecuteBeforeTestSuite,
    execInNewWindow : false })
    .addTestCase(languageNavigation.tcExecuteOpenCloseModal(tcExecuteBeforeTest, tcExecuteAfterTest))
    .addTestCase(languageNavigation.tcExecuteSwitchLanguage(tcExecuteBeforeTest, tcExecuteAfterTest));

}(hobs, jQuery));
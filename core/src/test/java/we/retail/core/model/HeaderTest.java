/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package we.retail.core.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.granite.security.user.UserManagementService;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class HeaderTest {

	@Rule
	public final AemContext context = AppAemContext.newAemContext();

	@Mock
	private UserManagementService ums;

	@Mock
	private LanguageManager languageManager;

	private Header header;

	@Before
	public void setup() {
		Page page = context.currentPage(Constants.TEST_HOME_PAGE);
		PageManager pageManager = context.pageManager();
		// ResourceResolver resourceResolver = context.resourceResolver();
		Resource resource = context.currentResource();

		// This sets the page attribute injected in Header with @ScriptVariable
		SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
		slingBindings.put(WCMBindings.CURRENT_PAGE, page);
		slingBindings.put(WCMBindings.PAGE_MANAGER, pageManager);
		slingBindings.put(WCMBindings.PROPERTIES, resource.getValueMap());
		
		context.request().setResource(resource);
		context.registerService(UserManagementService.class, ums);
		context.registerService(LanguageManager.class, languageManager);

		MockSlingHttpServletRequest request = context.request();
		header = request.adaptTo(Header.class);
	}

	@Test
	public void testUserPath() {
		String actualUserPath = header.getUserPath();
		Assert.assertEquals(StringUtils.EMPTY, actualUserPath);
	}

	@Test
	public void testIsModerator() {
		Assert.assertFalse(header.isModerator());
	}

	@Test
	public void testIsAnonymous() {
		Assert.assertTrue(header.isAnonymous());
	}

	@Test
	public void testCurrentPath() {
		String actualCurrentPath = header.getCurrentPath();
		Assert.assertEquals(Constants.TEST_HOME_PAGE, actualCurrentPath);
	}

	@Test
	public void testSignInPath() {
		String actualSignInPath = header.getSignInPath();
		Assert.assertEquals(Constants.HEADER_SIGNIN_PATH, actualSignInPath);
	}

	@Test
	public void testSignUpPath() {
		String actualSignUpPath = header.getSignUpPath();
		Assert.assertEquals(Constants.HEADER_SIGNUP_PATH, actualSignUpPath);
	}

	@Test
	public void testForgotPwdPath() {
		String actualForgotPwdPath = header.getForgotPwdPath();
		Assert.assertEquals(Constants.HEADER_FORGOT_PWD_PATH, actualForgotPwdPath);
	}

	@Test
	public void testMessagingPath() {
		String actualMessagingPath = header.getMessagingPath();
		Assert.assertEquals(Constants.HEADER_MESSAGING_PATH, actualMessagingPath);
	}

	@Test
	public void testNotificationPath() {
		String actualNotificationPath = header.getNotificationPath();
		Assert.assertEquals(Constants.HEADER_NOTIFICATION_PATH, actualNotificationPath);
	}

	@Test
	public void testModerationPath() {
		String actualModerationPath = header.getModerationPath();
		Assert.assertEquals(Constants.HEADER_MODERATION_PATH, actualModerationPath);
	}

	@Test
	public void testProfilePath() {
		String actualProfilePath = header.getProfilePath();
		Assert.assertEquals(Constants.HEADER_PROFILE_PATH, actualProfilePath);
	}

	@Test
	public void testAccountPath() {
		String actualAccountPath = header.getAccountPath();
		Assert.assertEquals(Constants.HEADER_ACCOUNT_PATH, actualAccountPath);
	}

	@Test
	public void testLanguageRoot() {

		String actualLanguageRoot = header.getLanguageRoot();
		Assert.assertEquals(Constants.HEADER_LANG_ROOT, actualLanguageRoot);
	}

	@Test
	public void testCurrentLanguageName() {
		String actualCurrentLanguage = header.getCurrentLanguage().getName();
		Assert.assertEquals(Constants.HEADER_CURR_LANG, actualCurrentLanguage);
	}

	@Test
	public void testNavigationResource() {
        Resource navigationResource = header.getNavigationResource();
        Assert.assertEquals("/conf/we-retail/settings/wcm/templates/hero-page/structure/jcr:content/root/header/navigation", navigationResource.getPath());
    }

}

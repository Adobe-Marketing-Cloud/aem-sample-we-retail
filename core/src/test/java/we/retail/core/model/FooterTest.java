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

import java.util.Calendar;
import java.util.List;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

public class FooterTest {

	@Rule
	public final AemContext context = AppAemContext.newAemContext();

	private Footer footer;

	@Before
	public void setup() {
		Page page = context.currentPage(Constants.TEST_HOME_PAGE);
		PageManager pageManager = context.pageManager();

		// This sets the page attribute injected in Footer with @ScriptVariable
		SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
		slingBindings.put(WCMBindings.CURRENT_PAGE, page);
		slingBindings.put(WCMBindings.PAGE_MANAGER, pageManager);

		MockSlingHttpServletRequest request = context.request();
		footer = request.adaptTo(Footer.class);
	}

	/**
	 * Test the currentYear value is same as present year in footer model.
	 */
	@Test
	public void testFooterDateCreated() {
		int yearActual = Calendar.getInstance().get(Calendar.YEAR);
		Assert.assertEquals(footer.getCurrentYear(),yearActual);
	}
	
	/**
	 * Test if the footer items are generated properly. The items in footer 
	 * should not be empty.
	 */
	@Test
	public void testFooterItems() {
		List<Page> itemActual = footer.getItems();
		Assert.assertFalse(itemActual.isEmpty());
	}
	
	/**
	 * Test the total number of items added in footer.
	 * The items in footer should be equal to child pages under root node.
	 */
	@Test
	public void testFooterItemCount() {
		List<Page> itemActual = footer.getItems();
		Assert.assertEquals(Constants.FOOTER_ITEM_SIZE, itemActual.size());
	}
}

/*
 *   Copyright 2016 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package we.retail.core.model;

import java.math.BigDecimal;

public class Constants {

    // The order is defined in src/test/resources/sample-order.json

    public static final String TEST_ORDER_ID = "5ad90db8-593f-4403-af91-2dd6df0aefd7";
    public static final String TEST_ORDER_RESOURCE = "/var/commerce/orders/2016/12/12/order";
    public static final String TEST_ORDER_PAGE = "/content/we-retail/us/en/user/account/order-history/order-details";

    public static final String SUB_TOTAL = "$167.00";
    public static final String SHIPPING_TOTAL = "$10.00";
    public static final String TAX_TOTAL = "$10.02";
    public static final String TOTAL = "$187.02";

    public static final BigDecimal SHIPPING_TOTAL_VALUE = new BigDecimal("10.00");
    public static final BigDecimal TAX_TOTAL_VALUE = new BigDecimal("10.02");

    public static final String BILLING_ADDRESS = "John Doe, 601 Townsend St 3rd floor, US-94103 San Francisco, CA";
    public static final String SHIPPING_ADDRESS = "John Doe, 94103 San Francisco, CA";
    public static final String ORDER_STATUS = "Processing";
    public static final String ORDER_DATE = "Mon Dec 12 2016 16:42:15 GMT+0100";
    public static final String ORDER_LIST_ID_INDEX = "1";

    public static final int ENTRIES_SIZE = 2;

    public static final String ENTRY_0_PATH = "/content/we-retail/us/en/products/equipment/running/fleet-cross-training-shoe/jcr:content/root/product/eqrusufle-9";
    public static final String ENTRY_0_PRICE = "$24.00";
    public static final String ENTRY_0_TOTAL_PRICE = "$48.00";
    public static final String ENTRY_0_SKU = "eqrusufle-9";
    public static final int ENTRY_0_QUANTITY = 2;

    public static final String ENTRY_1_PATH = "/content/we-retail/us/en/products/equipment/running/fleet-cross-training-shoe/jcr:content/root/product/eqrusufle-11";
    public static final String ENTRY_1_PRICE = "$119.00";
    public static final String ENTRY_1_TOTAL_PRICE = "$119.00";
    public static final String ENTRY_1_SKU = "eqrusufle-11";
    public static final int ENTRY_1_QUANTITY = 1;

    public static final String ORDER_ID = "orderId";
    public static final String ORDER_PLACED = "orderPlaced";
    
	public static final String TEST_HOME_PAGE = "/content/we-retail/us/en";

	public static final String HEADER_SIGNIN_PATH = "/content/we-retail/us/en/community/signin";
	public static final String HEADER_SIGNUP_PATH = "/content/we-retail/us/en/community/signup";
	public static final String HEADER_FORGOT_PWD_PATH = "/content/we-retail/us/en/community/useraccount/forgotpassword";
	public static final String HEADER_MESSAGING_PATH = "/content/we-retail/us/en/community/messaging";
	public static final String HEADER_NOTIFICATION_PATH = "/content/we-retail/us/en/community/notifications";
	public static final String HEADER_MODERATION_PATH = "/content/we-retail/us/en/community/moderation";
	public static final String HEADER_PROFILE_PATH = "/content/we-retail/us/en/community/profile";
	public static final String HEADER_ACCOUNT_PATH = "/content/we-retail/us/en/user/account";
	public static final String HEADER_THEME = "inverse";
  public static final String HEADER_LANG_ROOT = "/content/we-retail/us/en";
	public static final String HEADER_CURR_LANG = "English";
	public static final int HEADER_COUNTRY_SIZE = 7;
	public static final int HEADER_ITEM_SIZE = 6;

	public static final int FOOTER_ITEM_SIZE = 6;
}

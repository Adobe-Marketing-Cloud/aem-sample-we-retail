/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package apps.weretail.components.structure.footer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import we.retail.core.util.UrlHelper;
import we.retail.core.util.WeRetailHelper;

public class Footer extends WCMUsePojo {

    private List<Page> items = new ArrayList<Page>();
    private int currentYear;

    @Override
    public void activate() throws Exception {
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        populateItems();
    }

    private void populateItems() {
        Page rootPage = WeRetailHelper.findRoot(getCurrentPage());
        if (rootPage != null) {
            Iterator<Page> pageIterator = rootPage.listChildren(new PageFilter());
            while (pageIterator.hasNext()) {
                items.add(UrlHelper.resolveRedirectPage(pageIterator.next(), getPageManager()));
            }
        }
    }

    public List<Page> getItems() {
        return items;
    }

    public int getCurrentYear() {
        return currentYear;
    }
}

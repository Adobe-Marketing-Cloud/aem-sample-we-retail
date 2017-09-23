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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.wcm.core.components.models.List;
import com.day.cq.commons.Filter;
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.WCMException;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {List.class},
        resourceType = {"weretail/components/content/demolist"}
)
public class DemoListImpl implements List {
    @Override
    public Collection<Page> getItems() {
        return Arrays.asList(new Page[] {
                new DemoPage("Test1"),
                new DemoPage("Test2")
        });
    }

    @Override
    public boolean linkItems() {
        return false;
    }

    @Override
    public boolean showDescription() {
        return false;
    }

    @Override
    public boolean showModificationDate() {
        return false;
    }

    @Override
    public String getDateFormatString() {
        return null;
    }

    public class DemoPage implements Page {

        private String title;

        public DemoPage(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public PageManager getPageManager() {
            return null;
        }

        @Override
        public Resource getContentResource() {
            return null;
        }

        @Override
        public Resource getContentResource(String relPath) {
            return null;
        }

        @Override
        public Iterator<Page> listChildren() {
            return null;
        }

        @Override
        public Iterator<Page> listChildren(Filter<Page> filter) {
            return null;
        }

        @Override
        public Iterator<Page> listChildren(Filter<Page> filter, boolean deep) {
            return null;
        }

        @Override
        public boolean hasChild(String name) {
            return false;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public Page getParent() {
            return null;
        }

        @Override
        public Page getParent(int level) {
            return null;
        }

        @Override
        public Page getAbsoluteParent(int level) {
            return null;
        }

        @Override
        public ValueMap getProperties() {
            return null;
        }

        @Override
        public ValueMap getProperties(String relPath) {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getPageTitle() {
            return null;
        }

        @Override
        public String getNavigationTitle() {
            return null;
        }

        @Override
        public boolean isHideInNav() {
            return false;
        }

        @Override
        public boolean hasContent() {
            return false;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public long timeUntilValid() {
            return 0;
        }

        @Override
        public Calendar getOnTime() {
            return null;
        }

        @Override
        public Calendar getOffTime() {
            return null;
        }

        @Override
        public Calendar getDeleted() {
            return null;
        }

        @Override
        public String getDeletedBy() {
            return null;
        }

        @Override
        public String getLastModifiedBy() {
            return null;
        }

        @Override
        public Calendar getLastModified() {
            return null;
        }

        @Override
        public String getVanityUrl() {
            return null;
        }

        @Override
        public Tag[] getTags() {
            return new Tag[0];
        }

        @Override
        public void lock() throws WCMException {

        }

        @Override
        public boolean isLocked() {
            return false;
        }

        @Override
        public String getLockOwner() {
            return null;
        }

        @Override
        public boolean canUnlock() {
            return false;
        }

        @Override
        public void unlock() throws WCMException {

        }

        @Override
        public Template getTemplate() {
            return null;
        }

        @Override
        public Locale getLanguage(boolean ignoreContent) {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @CheckForNull
        @Override
        public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> type) {
            return null;
        }
    }
}

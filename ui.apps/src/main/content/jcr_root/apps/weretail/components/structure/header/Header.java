/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package apps.weretail.components.structure.header;

import java.lang.String;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.social.community.api.CommunityContext;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
import com.adobe.cq.sightly.WCMUsePojo;
import we.retail.core.util.WeRetailHelper;

public class Header extends WCMUsePojo {

    public static final Logger LOGGER = LoggerFactory.getLogger(Header.class);

    public static final String REDIRECT_RESOURCE_TYPE = "foundation/components/redirect";

    public static final String PROP_REDIRECT_TARGET = "redirectTarget";
    public static final String PROP_HIDE_IN_NAV = "hideInNav";
    public static final String PROP_HIDE_SUB_IN_NAV = "hideSubItemsInNav";


    public static final String SIGN_IN_PATH = "community/signin";
    public static final String SIGN_UP_PATH = "community/signup";
    public static final String FORGOT_PWD_PATH = "community/useraccount/forgotpassword";
    public static final String NOTIFICATION_PATH = "community/notifications";
    public static final String MODERATION_PATH = "community/moderation";
    public static final String MESSAGING_PATH = "community/messaging";
    public static final String PROFILE_PATH = "community/profile";
    public static final String ACCOUNT_PATH = "/content/we-retail/us/en/user/account";
    public static final String DEFAULT_ROOT_PATH = "/content/we-retail/us/en/";

    private ResourceResolver resolver;
    private Resource resource;
    private PageManager pageManager;
    private Page currentPage;
    private ValueMap properties;

    private boolean isModerator;
    private boolean isAnonymous;
    private String currentPath;
    private String signInPath;
    private String signUpPath;
    private String forgotPwdPath;
    private String messagingPath;
    private String notificationPath;
    private String moderationPath;
    private String profilePath;
    private String accountPath;
    private List<PagePojo> items;
    private String theme;
    private String languageRoot;
    private List<Country> countries;
    private Language currentLanguage;
    private String userPath;
    private Page root;
    
    @Override
    public void activate() throws Exception {
        resolver = getResourceResolver();
        resource = getResource();
        pageManager = getPageManager();
        currentPage = getCurrentPage();
        properties = getProperties();

        Page resourcePage = pageManager.getContainingPage(resource);
        if (resourcePage.getPath().startsWith("/conf/")) {
            resourcePage = currentPage;
        }

        root = WeRetailHelper.findRoot(resourcePage);
        languageRoot = "#";
        if (root != null) {
            items = getPages(root, 2, currentPage);
            if (!"/conf/".equals(root.getPath().substring(0, 6))) {
                languageRoot = root.getPath() + ".html";
            }
            countries = getCountries(root);
            currentLanguage = new Language(root.getPath(), root.getParent().getName(),
                    root.getName(), root.getTitle(), true);
        }

        isModerator = currentPage.adaptTo(CommunityContext.class)
                .checkIfUserIsModerator(resolver.adaptTo(UserManager.class), resolver.getUserID());
        isAnonymous = resolver.getUserID().equals("anonymous");
        currentPath = currentPage.getPath();
        signInPath = computePagePath(SIGN_IN_PATH);
        signUpPath = computePagePath(SIGN_UP_PATH);
        forgotPwdPath = computePagePath(FORGOT_PWD_PATH);
        messagingPath = computePagePath(MESSAGING_PATH);
        notificationPath = computePagePath(NOTIFICATION_PATH);
        moderationPath = computePagePath(MODERATION_PATH);
        profilePath = computePagePath(PROFILE_PATH);
        accountPath = ACCOUNT_PATH;
        theme = properties.get("theme", "inverse");
        userPath = resolver.adaptTo(UserManager.class).getAuthorizable(resolver.getUserID()).getPath();

        printDebug();
    }

    public String getUserPath() {
        return userPath;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getSignInPath() {
        return signInPath;
    }

    public String getSignUpPath() {
        return signUpPath;
    }

    public String getForgotPwdPath() {
        return forgotPwdPath;
    }

    public String getMessagingPath() {
        return messagingPath;
    }

    public String getNotificationPath() {
        return notificationPath;
    }

    public String getModerationPath() {
        return moderationPath;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public String getAccountPath() {
        return accountPath;
    }
    
    public List<PagePojo> getItems() {
        return items;
    }

    public String getTheme() {
        return theme;
    }

    public String getLanguageRoot() {
        return languageRoot;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public Language getCurrentLanguage() {
        return currentLanguage;
    }
    
    private String computePagePath(final String relativePath) {
        String computedPagePath;
        if (root != null) {
            computedPagePath = root.getPath() + "/" + relativePath;
            LOGGER.debug("Computed Path" +  computedPagePath);
            if (pageExists(computedPagePath)) {
                LOGGER.debug("Returning Computed Path "+ computedPagePath);
                return computedPagePath;
            }
        }
        LOGGER.debug("Returning default path "+ DEFAULT_ROOT_PATH + relativePath);
        return DEFAULT_ROOT_PATH + relativePath;
    }
    
    private boolean pageExists (final String pagePath) {
        if (pageManager != null) {
            Page page = pageManager.getPage(pagePath);
            if (page != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all the pages of a sub-tree
     * root - root node to start listing from
     * level - how deep to get into the tree
     */
    private List<PagePojo> getPages(Page root, int level, Page currentPage) {
        if (root == null || level == 0) {
            return null;
        }
        List<PagePojo> pages = new ArrayList<PagePojo>();
        Iterator<Page> it = root.listChildren(new PageFilter());

        while (it.hasNext()) {
            Page page = it.next();
            Resource pageContentResource = page.getContentResource();
            ValueMap pageValueMap = pageContentResource.adaptTo(ValueMap.class);
            if (pageValueMap.get(PROP_HIDE_IN_NAV, false)) {
                continue;
            }
            if (REDIRECT_RESOURCE_TYPE.equals(pageContentResource.getResourceType())) {
                page = resolveRedirect(pageValueMap);
            }
            boolean isSelected = (currentPage != null
                                    && page != null
                                    && currentPage.getPath().contains(page.getPath()));
            List<PagePojo> children = pageValueMap.get(PROP_HIDE_SUB_IN_NAV, false)
                    ? new ArrayList<PagePojo>()
                    : getPages(page, level - 1, currentPage);


            pages.add(new PagePojo(page, isSelected, children));
        }
        return pages;
    }

    /**
     * Returns the page, which the given page redirects to
     */
    private Page resolveRedirect(ValueMap pageValueMap) {
        String path = pageValueMap.get(PROP_REDIRECT_TARGET, String.class);
        return pageManager.getPage(path);
    }

    /**
     * Returns the list of countries supported by the site
     */
    private List<Country> getCountries(Page siteRoot) {
        List<Country> countries = new ArrayList<Country>();
        Page countryRoot = siteRoot.getParent(2);
        if (countryRoot == null) {
            return new ArrayList<Country>();
        }
        Iterator<Page> it = countryRoot.listChildren(new PageFilter());
        while (it.hasNext()) {
            Page countrypage = it.next();
            countries.add(new Country(countrypage.getName(), getLanguages(countrypage, siteRoot)));
        }
        return countries;
    }

    /**
     * Returns the list of languages supported by the site
     */
    private List<Language> getLanguages(Page countryRoot, Page siteRoot) {
        List<Language> languages = new ArrayList<Language>();
        Iterator<Page> langIt = countryRoot.listChildren(new PageFilter());
        while (langIt.hasNext()) {
            Page langPage = langIt.next();
            languages.add(new Language(langPage.getPath(), langPage.getParent().getName(), langPage.getName(),
                    langPage.getTitle(), siteRoot.getPath().equals(langPage.getPath())));
        }
        return languages;
    }

    private void printDebug() {
        LOGGER.debug("======================================");
        LOGGER.debug("userPath: {}", userPath);
        LOGGER.debug("isModerator: {}", isModerator);
        LOGGER.debug("isAnonymous: {}", isAnonymous);
        LOGGER.debug("currentPath: {}", currentPath);
        LOGGER.debug("signInPath: {}", signInPath);
        LOGGER.debug("signUpPath: {}", signUpPath);
        LOGGER.debug("forgotPwdPath: {}", forgotPwdPath);
        LOGGER.debug("messagingPath: {}", messagingPath);
        LOGGER.debug("notificationPath: {}", notificationPath);
        LOGGER.debug("profilePath: {}", profilePath);
        LOGGER.debug("theme: {}", theme);
        LOGGER.debug("languageRoot: {}", languageRoot);
        if (currentLanguage != null) {
            LOGGER.debug("currentLanguage: {}", currentLanguage.getName());
        }

        if (items != null && !items.isEmpty()) {
            for (PagePojo item: items) {
                LOGGER.debug("page-path: {}", item.getPage().getPath());
            }
        }

        if (countries != null && !countries.isEmpty()) {
            for (Country country: countries) {
                LOGGER.debug("country-code: {}", country.getCountrycode());
            }
        }
    }


    // --------------------------------------- nested class: Country  --------------------------------------- //

    public class Country {

        private String countrycode;
        private List<Language> languages;

        public Country(String countrycode, List<Language> languages) {
            this.countrycode = countrycode;
            this.languages = languages;
        }

        public String getCountrycode() {
            return countrycode;
        }

        public List<Language> getLanguages() {
            return languages;
        }
    }


    // --------------------------------------- nested class: Language  --------------------------------------- //

    public class Language {

        private String path;
        private String countrycode;
        private String languagecode;
        private String name;
        private boolean selected;

        public Language(String path, String countrycode, String languagecode, String name, boolean selected) {
            this.path = path;
            this.countrycode = countrycode;
            this.languagecode = languagecode;
            this.name = name;
            this.selected = selected;
        }

        public String getPath() {
            return path;
        }

        public String getCountrycode() {
            return countrycode;
        }

        public String getLanguagecode() {
            return languagecode;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }
    }


    // --------------------------------------- nested class: PagePojo  --------------------------------------- //


    public class PagePojo {

        private Page page;
        private boolean selected;
        private List<PagePojo> children;

        public PagePojo(Page page, boolean selected, List<PagePojo> children) {
            this.page = page;
            this.selected = selected;
            this.children = children;
        }

        public Page getPage() {
            return page;
        }

        public boolean isSelected() {
            return selected;
        }

        public List<PagePojo> getChildren() {
            return children;
        }

    }

}
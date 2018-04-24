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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.social.community.api.CommunityContext;
import com.adobe.granite.security.user.UserManagementService;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import we.retail.core.util.WeRetailHelper;

@Model(adaptables = {SlingHttpServletRequest.class})
public class Header {
    public static final Logger LOGGER = LoggerFactory.getLogger(Header.class);

    public static final String REDIRECT_RESOURCE_TYPE = "weretail/components/structure/page";

    public static final String PROP_REDIRECT_TARGET = "cq:redirectTarget";
    public static final String PROP_HIDE_SUB_IN_NAV = "hideSubItemsInNav";

    public static final String SIGN_IN_PATH = "community/signin";
    public static final String SIGN_UP_PATH = "community/signup";

    @SuppressWarnings("squid:S2068")
    public static final String FORGOT_PWD_PATH = "community/useraccount/forgotpassword";
    public static final String NOTIFICATION_PATH = "community/notifications";
    public static final String MODERATION_PATH = "community/moderation";
    public static final String MESSAGING_PATH = "community/messaging";
    public static final String PROFILE_PATH = "community/profile";
    public static final String ACCOUNT_PATH = "/content/we-retail/us/en/user/account";
    public static final String DEFAULT_ROOT_PATH = "/content/we-retail/us/en/";
    private static final String NN_NAVIGATION = "navigation";
    private static final String NAVIGATION_TEMPLATE_RESOURCE_PATH = "structure/jcr:content/root/header/" + NN_NAVIGATION;

    @SlingObject
    private ResourceResolver resolver;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private PageManager pageManager;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    @SlingObject
    private SlingScriptHelper slingScriptHelper;

    @OSGiService
    private LanguageManager languageManager;

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
    private String languageRoot;
    private Language currentLanguage;
    private boolean isCommunitiesPage;
    private String userPath;
    private Page root;
    private UserManagementService ums;
    private Resource navigationResource;

    @PostConstruct
    private void initModel() {
        try {
            Page resourcePage = pageManager.getContainingPage(resource);
            if (resourcePage.getPath().startsWith("/conf/")) {
                resourcePage = currentPage;
            }

            root = WeRetailHelper.findRoot(resourcePage);
            languageRoot = "#";
            if (root != null) {
                if (!"/conf/".equals(root.getPath().substring(0, 6))) {

                    languageRoot = root.getPath();
                }
                currentLanguage = new Language(root.getPath(), root.getParent().getName(), root.getName(),
                        root.getTitle(), true);
            }

            ums = slingScriptHelper.getService(UserManagementService.class);
            String anonymousId = ums != null ? ums.getAnonymousId() : UserConstants.DEFAULT_ANONYMOUS_ID;
            String userId = resolver.getUserID();

            CommunityContext communityContext = currentPage.adaptTo(CommunityContext.class);
            if (communityContext != null) {
                isModerator = communityContext.checkIfUserIsModerator(resolver.adaptTo(UserManager.class), userId);
            }
            isAnonymous = userId == null || userId.equals(anonymousId);
            currentPath = currentPage.getPath();
            signInPath = computePagePath(SIGN_IN_PATH);
            signUpPath = computePagePath(SIGN_UP_PATH);
            forgotPwdPath = computePagePath(FORGOT_PWD_PATH);
            messagingPath = computePagePath(MESSAGING_PATH);
            notificationPath = computePagePath(NOTIFICATION_PATH);
            moderationPath = computePagePath(MODERATION_PATH);
            profilePath = computePagePath(PROFILE_PATH);
            accountPath = ACCOUNT_PATH;

            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager != null) {
                userPath = userManager.getAuthorizable(userId).getPath();
            }
            isCommunitiesPage = currentPage.getPath().startsWith(languageRoot + "/community");

            navigationResource = resource.getChild(NN_NAVIGATION);
            if (navigationResource == null) {
                Template template = currentPage.getTemplate();
                if (isCommunitiesPage && languageManager != null) {
                    Page languageRoot = languageManager.getLanguageRoot(currentPage.getContentResource());
                    if (languageRoot != null) {
                        template = languageRoot.getTemplate();
                    }
                }
                if (template != null) {
                    Resource templateResource = resolver.getResource(template.getPath());
                    if (templateResource != null) {
                        navigationResource = templateResource.getChild(NAVIGATION_TEMPLATE_RESOURCE_PATH);
                    }
                }
            }

            if (LOGGER.isDebugEnabled()) {
                printDebug();
            }
        } catch (RepositoryException e) {
            LOGGER.error("Failed to initialize sling model", e);
        }
    }

    public Resource getNavigationResource() {
        return navigationResource;
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

    public String getLanguageRoot() {
        return languageRoot;
    }

    public Language getCurrentLanguage() {
        return currentLanguage;
    }

    public boolean isCommunitiesPage() {
        return isCommunitiesPage;
    }

    private String computePagePath(final String relativePath) {
        String computedPagePath;
        if (root != null) {
            computedPagePath = root.getPath() + "/" + relativePath;
            LOGGER.debug("Computed Path" + computedPagePath);
            if (pageExists(computedPagePath)) {
                LOGGER.debug("Returning Computed Path " + computedPagePath);
                return computedPagePath;
            }
        }
        LOGGER.debug("Returning default path " + DEFAULT_ROOT_PATH + relativePath);
        return DEFAULT_ROOT_PATH + relativePath;
    }

    private boolean pageExists(final String pagePath) {
        return pageManager != null && pageManager.getPage(pagePath) != null;
    }

    /**
     * Returns all the pages of a sub-tree root - root node to start listing
     * from level - how deep to get into the tree
     */
    private List<PagePojo> getPages(Page root, int level, Page currentPage) {
        if (root == null || level == 0) {
            return null;
        }
        List<PagePojo> pages = new ArrayList<PagePojo>();
        Iterator<Page> it = root.listChildren(new PageFilter());

        while (it.hasNext()) {
            Page page = it.next();
            ValueMap pageValueMap = page.getProperties();
            if (REDIRECT_RESOURCE_TYPE.equals(page.getContentResource().getResourceType()) &&
                    pageValueMap.get(PROP_REDIRECT_TARGET) != null) {
                page = resolveRedirect(pageValueMap);
            }
            boolean isSelected = (currentPage != null && page != null
                    && currentPage.getPath().contains(page.getPath()));
            List<PagePojo> children = pageValueMap.get(PROP_HIDE_SUB_IN_NAV, false) ? new ArrayList<PagePojo>()
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
        LOGGER.debug("languageRoot: {}", languageRoot);
        if (currentLanguage != null) {
            LOGGER.debug("currentLanguage: {}", currentLanguage.getName());
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
            this.countrycode = countrycode.toUpperCase();
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
            this.children = new ArrayList<>(children);
        }

        public Page getPage() {
            return page;
        }

        public boolean isSelected() {
            return selected;
        }

        public List<PagePojo> getChildren() {
            return Collections.unmodifiableList(children);
        }

    }

}

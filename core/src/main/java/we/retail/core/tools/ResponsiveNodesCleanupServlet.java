/*
 *   Copyright 2017 Adobe Systems Incorporated
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
package we.retail.core.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.query.Query;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up cq:responsive nodes
 */
@SuppressWarnings("serial")
@Component
@Service
@Properties(value={
        @Property(name = "service.description", value = "Cleans up cq:responsive nodes."),
        @Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default"),
        @Property(name = "sling.servlet.selectors", value = { "responsiveCleanup" }),
        @Property(name = "sling.servlet.extensions", value = {"html"}),
        @Property(name = "sling.servlet.methods", value = "GET")
})
public class ResponsiveNodesCleanupServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponsiveNodesCleanupServlet.class);

    private final static String JCR_ROOT = "/jcr:root";
    private final static String NT_BASE = "nt:base";
    private final static String CONTENT_ROOT = "/content";
    private final static String CONF_ROOT = "/conf";
    private final static String NN_RESPONSIVE = "cq:responsive";
    private final static String PN_WIDTH = "width";

    private final static String PARAM_TYPE = "type";
    private final static String PARAM_VIEW_TYPE = "viewType";
    private final static String PARAM_OPERATION = "operation";
    private final static String PARAM_SEARCH_IN = "searchIn";

    private final static String PARAM_VALUE_SHADOW = "shadow";
    private final static String PARAM_VALUE_SHADOW_IN_TEMPLATE_INITIAL = "shadowInTemplateInitial";
    private final static String PARAM_VALUE_SHADOW_IN_CONTENT = "shadowInContent";
    private final static String PARAM_VALUE_ORPHAN = "orphan";

    private final static String PARAM_VALUE_NO_VIEW = "noView";
    private final static String PARAM_VALUE_SIMPLE = "simple";
    private final static String PARAM_VALUE_DETAILED = "detailed";

    private final static String PARAM_VALUE_VIEW = "view";
    private final static String PARAM_VALUE_DELETE = "delete";

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();

        String responsiveType = request.getParameter(PARAM_TYPE);
        String viewType = request.getParameter(PARAM_VIEW_TYPE);
        String operation = request.getParameter(PARAM_OPERATION);
        RequestParameter[] searchInParams = request.getRequestParameters(PARAM_SEARCH_IN);
        List<String> searchPaths = new ArrayList<String>();

        if (StringUtils.isEmpty(responsiveType)) {
            responsiveType = PARAM_VALUE_SHADOW;
        }
        if (StringUtils.isEmpty(viewType)) {
            viewType = PARAM_VALUE_SIMPLE;
        }
        if (StringUtils.isEmpty(operation)) {
            operation = PARAM_VALUE_VIEW;
        }
        if (searchInParams == null || searchInParams.length == 0) {
            searchPaths.add(CONTENT_ROOT);
            searchPaths.add(CONF_ROOT);
        } else {
            for (RequestParameter searchInParam : searchInParams) {
                searchPaths.add(searchInParam.getString());
            }
        }

        List<ResponsiveItem> responsiveItems = getResponsiveItems(resolver, responsiveType, searchPaths);

        if (PARAM_VALUE_VIEW.equals(operation)) {
            displayItems(response, responsiveItems, responsiveType, searchPaths, viewType);
        } else if (PARAM_VALUE_DELETE.equals(operation)) {
            displayTitle("Deleting the following responsive nodes: \n", response);
            displayItems(response, responsiveItems, responsiveType, searchPaths, viewType);
            removeItems(resolver, responsiveItems);
            displayTitle("The responsive nodes have been deleted. \n", response);
        }

        displayDocumentation(response);

    }

    private List<ResponsiveItem> getResponsiveItems(ResourceResolver resolver, String responsiveType, List<String> searchPaths) {
        List<ResponsiveItem> responsiveItems = new ArrayList<ResponsiveItem>();
        for (String searchPath : searchPaths) {
            if (StringUtils.isNotEmpty(searchPath) && !"/".equals(searchPath) && resolver.getResource(searchPath) != null) {
                String query = JCR_ROOT + searchPath + "//element(" + NN_RESPONSIVE + "," + NT_BASE + ")";
                Iterator<Resource> it = resolver.findResources(query, Query.XPATH);
                while (it.hasNext()) {
                    Resource res = it.next();
                    ResponsiveItem responsiveItem = new ResponsiveItem(res);
                    if (itemMatchesType(responsiveItem, responsiveType)) {
                        responsiveItems.add(responsiveItem);
                    }
                }
            }
        }
        return responsiveItems;
    }

    private boolean itemMatchesType(ResponsiveItem item, String type) {
        if (item != null) {
            if (PARAM_VALUE_SHADOW.equals(type)) {
                return item.isShadow();
            }
            if (PARAM_VALUE_SHADOW_IN_TEMPLATE_INITIAL.equals(type)) {
                return item.isShadowInTemplateInitial();
            }
            if (PARAM_VALUE_SHADOW_IN_CONTENT.equals(type)) {
                return item.isShadowInContent();
            }
            if (PARAM_VALUE_ORPHAN.equals(type)) {
                return item.isOrphan();
            }
        }
        return false;
    }

    private void removeItems(ResourceResolver resolver, List<ResponsiveItem> responsiveItems) {
        for (ResponsiveItem item: responsiveItems) {
            try {
                resolver.delete(item.getResource());
                resolver.commit();
            } catch (PersistenceException e) {
                LOGGER.error("Error deleting the resource at {}", item.getResource().getPath());
            }
        }
    }

    private void displayItems(SlingHttpServletResponse response, List<ResponsiveItem> responsiveItems, String responsiveType,
                              List<String> searchPaths, String viewType) throws IOException{

        boolean noView = PARAM_VALUE_NO_VIEW.equals(viewType);
        boolean simpleView = PARAM_VALUE_SIMPLE.equals(viewType);
        boolean detailedView = PARAM_VALUE_DETAILED.equals(viewType);

        if (noView) {
            return;
        }

        StringBuilder searchPathsString = new StringBuilder("{");
        int idx = 1;
        for (String path : searchPaths) {
            searchPathsString.append(path);
            if (idx < searchPaths.size()) {
                searchPathsString.append(",");
            }
            idx++;
        }
        searchPathsString.append("}");

        displayTitle("=============================================================================", response);
        displayTitle("Welcome to the Responsive Nodes Clean Up Tool \n", response);
        displayTitle("Please read the documentation at the end of the page. \n", response);
        displayTitle("=============================================================================", response);
        displayTitle("Responsive nodes meeting the following criteria:", response);
        displayTitle("- type: " + responsiveType, response);
        displayTitle("- viewType: " + viewType, response);
        displayTitle("- searchPaths: " + searchPathsString.toString(), response);
        displayTitle("=============================================================================", response);
        displayTitle("", response);


        if (responsiveItems.isEmpty()) {
            displayTitle("-> this category is empty.", response);
        }
        for (ResponsiveItem item: responsiveItems) {
            if (simpleView) {
                response.getWriter().write(item.getPath() + "\n");
            } else if (detailedView) {
                displayTitle("--------------------------", response);
                displayTitle("responsive node:", response);
                response.getWriter().write(item.getPath() + "\n");
                displayResponsiveConfig(item.getResource(), response);
                displayTitle("", response);

                displayTitle("template structure node:", response);
                Resource templateStructureEditable = item.getTemplateStructureEditable();
                if (templateStructureEditable != null) {
                    response.getWriter().write(templateStructureEditable.getPath() + "\n");
                    Resource templateStructureResponsive = templateStructureEditable.getChild(NN_RESPONSIVE);
                    if (templateStructureResponsive != null) {
                        displayResponsiveConfig(templateStructureResponsive, response);
                    } else {
                        displayTitle("no cq:responsive node", response);
                    }
                    response.getWriter().write("\n");
                }
            }
        }
        response.getWriter().write("\n");
    }

    private void displayResponsiveConfig(Resource resource, SlingHttpServletResponse response) throws IOException{
        Iterator<Resource> children = resource.listChildren();
        String spacer1 = "  ";
        String spacer2 = "    ";
        while (children.hasNext()) {
            Resource child = children.next();
            String name = child.getName();
            displayTitle(spacer1 + "+ " + name, response);
            ValueMap vm = child.getValueMap();
            String width = vm.get(PN_WIDTH, "");
            displayTitle(spacer2 + "- width: " + width, response);
        }
    }

    private void displayTitle(String title, SlingHttpServletResponse response) throws IOException {
        response.getWriter().write(title + "\n");
    }

    private void displayDocumentation(SlingHttpServletResponse response) throws IOException {
        String doc = "" +
                "" +
                "============================================================================= \n" +
                "Documentation about the Responsive Nodes Clean Up Tool \n" +
                "\n" +
                "Available URL parameters when using the 'responsiveCleanup' selector: \n" +
                "\n" +
                "'type': defines the type of the responsive nodes. Possible values: \n" +
                "- shadow: see [0]\n" +
                "- shadowInTemplateInitial: shadow responsive nodes that are part of a template initial page\n" +
                "- shadowInContent: shadow responsive nodes that are below /content\n" +
                "- orphan: see [1] \n" +
                "\n" +
                "'viewType': defines how to display the nodes. Possible values: \n" +
                "- noView: displays blank\n" +
                "- simple: displays the list of nodes\n" +
                "- detailed: displays the list of nodes with their configurations\n" +
                "\n" +
                "'searchIn' parameter. Defines the paths to search in (e.g.: /conf). Multiple values are possible: \n" +
                "- the root path ('/') is not allowed and will be skipped.\n" +
                "\n" +
                "'operation': defines which operation to perform. Possible values: \n" +
                "- view: displays the nodes\n" +
                "- delete: deletes the nodes\n" +
                "\n" +
                "[0] responsive node (node name = cq:responsive) that has a twin node in the corresponding template \n" +
                "structure (<template>/structure), for which the twin node has an editable parent (editable=true)\n" +
                "\n" +
                "[1] responsive node (node name = cq:responsive) that does not have a twin node in the corresponding \n" +
                "template structure (<template>/structure), but has an editable parent (editable=true)\n" +
                "";
        response.getWriter().write(doc + "\n");
    }

}

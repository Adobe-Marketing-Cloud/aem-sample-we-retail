package we.retail.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/**
 * Created by Daniel on 03/12/15.
 */
@Model(adaptables = {Resource.class})
public class Article {
	
	static Logger LOGGER = LoggerFactory.getLogger(Article.class); 
	
    @Inject
    @SlingObject
    private ResourceResolver resourceResolver;
    
    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_TITLE)
    @Default(values = "")
    public String title;

    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_DESCRIPTION)
    @Optional
    public String description;

    @Inject
    @Named(JcrConstants.JCR_CONTENT + "/cq:tags")
    @Default(values = {})
    public String[] cqTags;
    
    public Resource resource;
    
    public Article(Resource resource) {
    	this.resource = resource;
    }
    
    public String getImagePath() {
    	String path = resource.getPath() + ".thumb.319.319.jpg";
    	
    	Resource heroImageResource = resource.getChild(JcrConstants.JCR_CONTENT + "/root/hero_image");
    	String heroFileReference = heroImageResource.adaptTo(ValueMap.class).get("fileReference", String.class);
    	
    	if(heroFileReference != null) {
    		path = heroFileReference;
    	}
    	
    	Resource thumbnailImageResource = resource.getChild(JcrConstants.JCR_CONTENT + "/image");
    	String thumbnailFileReference = thumbnailImageResource.adaptTo(ValueMap.class).get("fileReference", String.class);
    	
    	if(thumbnailFileReference != null) {
    		path = thumbnailFileReference;
    	}
    	
    	return path;
    }

    public List<Tag> getTags() {
        List<Tag> tags = new ArrayList<Tag>();
        TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
        
        try {
	    	Resource contentFragmentResource = getContentFragment();
	    	
	    	if(contentFragmentResource != null) {
		    	ValueMap contentFragmentMetadata = contentFragmentResource.adaptTo(ValueMap.class);
		    	String[] tagIds = contentFragmentMetadata.get("cq:tags", String[].class);
		    	
		    	for (String cqTag : tagIds) {
		            tags.add(tagManager.resolve(cqTag));
		        }
	    	}
	    }
		catch (RepositoryException ex) {
			LOGGER.error("Error getting article tags", ex);
		}

        return tags;
    }
    
    public Profile getAuthor() {
    	Profile author = null;
    	
    	try {
	    	Resource contentFragmentResource = getContentFragment();
	    	
	    	if(contentFragmentResource != null) {
		    	ValueMap contentFragmentMetadata = contentFragmentResource.adaptTo(ValueMap.class);
		    	// TODO: find the right property to get the author id from
		    	String authorId = contentFragmentMetadata.get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
		    	
				UserManager userManager = resourceResolver.adaptTo(UserManager.class);
				String authorPath = userManager.getAuthorizable(authorId).getPath();
				
				author = resourceResolver.getResource(authorPath).adaptTo(Profile.class);
	    	}
	    }
		catch (RepositoryException ex) {
			LOGGER.error("Error getting article author", ex);
		}
    	
    	return author;
    }
    
    public String getModified() {
    	String result = null;
    	
    	try {
	    	Resource contentFragmentResource = getContentFragment();
	    	
	    	if(contentFragmentResource != null) {
		    	ValueMap contentFragmentMetadata = contentFragmentResource.adaptTo(ValueMap.class);	
		    	Date modified = contentFragmentMetadata.get("dc:modified", Date.class);
		    	
		    	result = new SimpleDateFormat("MMM dd, YYYY", Locale.US).format(modified);
	    	}
	    }
		catch (RepositoryException ex) {
			LOGGER.error("Error getting article creation date", ex);
		}
    	
    	return result;
    }
    
    private Resource getContentFragment() throws RepositoryException {
    	Resource result = null;
    			
    	Map<String, String> params = new HashMap<String, String>();
    	params.put("path", resource.getPath());
    	params.put("property", "sling:resourceType");
    	params.put("property.value", "dam/cfm/components/contentfragment");
    	
		QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
    	Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));                   
        SearchResult searchResult = query.getResult();
    	List<Hit> hits = searchResult.getHits();
    	
        if(hits.size() > 0) {
        	ValueMap fragmentProperties = hits.get(0).getResource().adaptTo(ValueMap.class);
        	String fragmentReference = fragmentProperties.get("fileReference", String.class);
        	
        	if(fragmentReference != null) {
        		result = resourceResolver.getResource(fragmentReference + "/jcr:content/metadata");
        	}
        }
    	
    	return result;
    }
}

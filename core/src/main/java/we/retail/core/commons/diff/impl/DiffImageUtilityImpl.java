/**
 * 
 */
package we.retail.core.commons.diff.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.commons.DiffService;
import com.day.cq.wcm.foundation.Image;

/**
 * Type to compare text. Implement any changes customisation for image
 * comparison here.
 * 
 * @author vvenkata
 *
 */
public class DiffImageUtilityImpl extends DiffUtilityImpl {

	public DiffImageUtilityImpl(Resource resource, DiffService diffService,
			String vLabel, String fallbackValue, Boolean isRich) {
		super(resource, diffService, vLabel, fallbackValue, isRich);
	}

	/**
	 * This method is to return the value of text property in JCR As noticed
	 * small change here to first get the image from location sitting under the
	 * version resource node and get the location to compare.
	 * 
	 * @param propertyToDiff
	 * @param vRes
	 * @param map
	 * @param diffText
	 * @return
	 */
	public String getTextFromVersionToDiff(final String propertyToDiff,
			Resource vRes, ValueMap map, String diffText) {
		Resource imageResource = vRes.getChild(propertyToDiff);

		if (imageResource != null) {
			Image image = new Image(imageResource);
			diffText = image.getFileReference();
		}

		LOG.debug(
				"getTextFromVersionToDiff -- diffText from version to be compared against is {}",
				diffText);

		return diffText;
	}

}

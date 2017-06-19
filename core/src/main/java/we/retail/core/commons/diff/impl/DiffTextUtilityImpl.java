/**
 * 
 */
package we.retail.core.commons.diff.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.commons.DiffService;

/**
 * Type to compare text. Implement any changes customisation for text comparison here.
 * 
 * @author vvenkata
 *
 */
public class DiffTextUtilityImpl extends DiffUtilityImpl {

	public DiffTextUtilityImpl(Resource resource, DiffService diffService,
			String vLabel, String fallbackValue, Boolean isRich) {
		super(resource, diffService, vLabel, fallbackValue, isRich);
	}

	/**
	 * This method is to return the value of text property in JCR
	 * 
	 * @param propertyToDiff
	 * @param vRes
	 * @param map
	 * @param diffText
	 * @return
	 */
	public String getTextFromVersionToDiff(final String propertyToDiff,
			Resource vRes, ValueMap map, String diffText) {
		diffText = map.get(propertyToDiff, StringUtils.EMPTY);
		LOG.debug(
				"getTextFromVersionToDiff -- diffText from version to be compared against is {}",
				diffText);
		return diffText;
	}

}

/**
 * 
 */
package we.retail.core.commons.diff.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.retail.core.commons.diff.DiffUtility;

import com.day.cq.commons.DiffInfo;
import com.day.cq.commons.DiffService;

/**
 * DiffUtility to help with basic text difference.
 * 
 */
public abstract class DiffUtilityImpl implements DiffUtility {
	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private Resource resource;
	private DiffService diffService;
	private String versionToDiffWithFromUrlParam = null;
	private String fallbackValue = StringUtils.EMPTY;
	private Boolean isRich = false;

	/**
	 * @param resource
	 * @param diffService
	 * @param versionToDiffWithFromUrlParam
	 */
	public DiffUtilityImpl(Resource resource, DiffService diffService,
			String versionToDiffWithFromUrlParam, String fallbackValue,
			Boolean isRich) {
		super();
		this.resource = resource;
		this.diffService = diffService;
		this.versionToDiffWithFromUrlParam = versionToDiffWithFromUrlParam;
		this.isRich = isRich;
		this.fallbackValue = fallbackValue;
	}

	/**
	 * DiffUtility to help with basic text difference.
	 * 
	 * Usage method
	 * 
	 * <div data-sly-test="${contact.diffButtonLabel}"
	 * class="content">${contact.diffButtonLabel @ context='unsafe'}</div>
	 * 
	 * 
	 * This proves it again, everything usually done with scriptlets or with
	 * TagLibs can be done with the Use-API and/or a data-sly-template and/or
	 * Sling Models
	 * 
	 * @param diffRequestParameters
	 *            Send the request parameters from the URL for name ?cq_diffTo.
	 *            This will be the version to be compared with
	 * @param currentValue
	 *            The current value of the property
	 * @param propertyToDiff
	 *            Property which needs to be diff'd
	 * @param fallbackValue
	 *            Fallback value if in case the diff return nothing
	 * @param isRich
	 *            Whethere we are doing a diff of Rich text
	 * @param resource
	 *            Resource of the node
	 * @param diffService
	 *            AEM diff service
	 * @return
	 */
	@Override
	public final String getDiff(final String currentValue,
			final String propertyToDiff) {
		LOG.debug("START - getDiff for property {} & currentValue {}",
				propertyToDiff, currentValue);
		String output = null;

		// 1. Make sure the resource node under consideration is NOT NULL
		if (getResource() != null) {
			// 2. Make sure we are in diff mode (indicated by url with param
			// cq_diffTo)
			if (isDiffMode()) {
				// 3. handle the case when title component is not inside parsys.
				// So get the version of the page from service
				Resource versionToDiffWithResource = DiffInfo
						.getVersionedResource(getResource(),
								versionToDiffWithFromUrlParam);

				// 4. handle normal case
				if (versionToDiffWithResource == null
						&& resource.getParent() != null) {
					versionToDiffWithResource = DiffInfo.getVersionedResource(
							getResource().getParent(),
							versionToDiffWithFromUrlParam);
				}

				LOG.debug("getDiff -- Received resource. Now doing diff");

				// 5. Now we have the version resource, get the property to do
				// comparison on and do compare and send back results.
				if (versionToDiffWithResource != null) {

					ValueMap map = ResourceUtil
							.getValueMap(versionToDiffWithResource);
					String valueFromVersionComparedWith = StringUtils.EMPTY;

					valueFromVersionComparedWith = getTextFromVersionToDiff(
							propertyToDiff, versionToDiffWithResource, map,
							valueFromVersionComparedWith);

					LOG.debug(
							"getDiff -- diffText from version to be compared against is {}",
							valueFromVersionComparedWith);

					output = getDiffService().diff(currentValue,
							valueFromVersionComparedWith, getIsRich());

					LOG.debug("getDiff -- Output from comparison {}", output);

				}
			}
		}

		// 6. One final check to make sure only the compared result and
		// variations
		// are outputted.
		if (StringUtils.isNotBlank(output)
				&& StringUtils.equalsIgnoreCase(output, currentValue)) {
			LOG.debug("getDiff -- Since output is same as original, resetting output to NULL");
			output = null;
		}

		LOG.debug("END - getDiff for property " + propertyToDiff
				+ " & currentValue " + currentValue + ". Output is " + output);
		return output;
	}

	@Override
	public Boolean isDiffMode() {
		return StringUtils.isNotBlank(versionToDiffWithFromUrlParam)
				&& getDiffService() != null;
	}

	/**
	 * This method will vary depending on image or text. Look to implementing
	 * class for information.
	 * 
	 * @param propertyToDiff
	 * @param vRes
	 * @param map
	 * @param diffText
	 * @return
	 */
	public abstract String getTextFromVersionToDiff(
			final String propertyToDiff, Resource vRes, ValueMap map,
			String diffText);

	/**
	 * @return the resource
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @return the diffService
	 */
	public DiffService getDiffService() {
		return diffService;
	}

	/**
	 * @return the vLabel
	 */
	public String getvLabel() {
		return versionToDiffWithFromUrlParam;
	}

	/**
	 * @param resource
	 *            the resource to set
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @param diffService
	 *            the diffService to set
	 */
	public void setDiffService(DiffService diffService) {
		this.diffService = diffService;
	}

	/**
	 * @param vLabel
	 *            the vLabel to set
	 */
	public void setvLabel(String vLabel) {
		this.versionToDiffWithFromUrlParam = vLabel;
	}

	/**
	 * @return the fallbackValue
	 */
	public final String getFallbackValue() {
		return fallbackValue;
	}

	/**
	 * @return the isRich
	 */
	public final Boolean getIsRich() {
		return isRich;
	}

	/**
	 * @param fallbackValue
	 *            the fallbackValue to set
	 */
	public final void setFallbackValue(String fallbackValue) {
		this.fallbackValue = fallbackValue;
	}

	/**
	 * @param isRich
	 *            the isRich to set
	 */
	public final void setIsRich(Boolean isRich) {
		this.isRich = isRich;
	}
}

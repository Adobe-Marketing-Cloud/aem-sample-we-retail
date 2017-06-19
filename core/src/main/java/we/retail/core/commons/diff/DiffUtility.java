package we.retail.core.commons.diff;

/*
 * 
 * DiffUtility to help with basic text difference.
 */
public interface DiffUtility {

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
	public abstract String getDiff(final String currentValue,
			final String propertyToDiff);

	public Boolean isDiffMode();
	
}
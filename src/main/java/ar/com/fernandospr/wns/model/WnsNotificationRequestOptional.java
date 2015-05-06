package ar.com.fernandospr.wns.model;

import ar.com.fernandospr.wns.model.types.WnsCachePolicyTypeEnum;
import ar.com.fernandospr.wns.model.types.WnsRequestForStatusTypeEnum;

/**
 * Optional headers from <a href="http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request">http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request</a>
 */
public class WnsNotificationRequestOptional {
	/**
	 * Should be any of {@link ar.com.fernandospr.wns.model.types.WnsCachePolicyTypeEnum}
	 */
	public WnsCachePolicyTypeEnum cachePolicy;
	
	/**
	 * Should be any of {@link ar.com.fernandospr.wns.model.types.WnsRequestForStatusTypeEnum}
	 */
	public WnsRequestForStatusTypeEnum requestForStatus;
	
	public String tag;
	
	public String ttl;
}

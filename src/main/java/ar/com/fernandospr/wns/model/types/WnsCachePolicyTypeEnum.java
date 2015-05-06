package ar.com.fernandospr.wns.model.types;

/**
 * From <a href="http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request">http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request</a>
 */
public enum WnsCachePolicyTypeEnum{
	DEFAULT(0, ""),
	CACHE(1,"cache"),
	NOCACHE(2,"no-cache")
	;

	private int type;
	private String label;
	
	private WnsCachePolicyTypeEnum(int type, String label)
	{
	    this.type = type;
	    this.label = label;
	}

}
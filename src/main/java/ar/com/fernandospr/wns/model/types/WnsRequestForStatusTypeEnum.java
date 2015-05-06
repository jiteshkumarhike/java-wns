package ar.com.fernandospr.wns.model.types;

/**
 * From <a href="http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request">http://msdn.microsoft.com/en-us/library/windows/apps/hh465435.aspx#send_notification_request</a>
 */
public enum WnsRequestForStatusTypeEnum {
	DEFAULT(0,""),
	TRUE(1,"true"),
	FALSE(2,"false")
	;
	
	private int type;
	private String label;
	private WnsRequestForStatusTypeEnum(int type, String label)
	{
		this.type = type;
		this.label = label;
	}
	
}

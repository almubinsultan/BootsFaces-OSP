package net.bootsfaces.component.growl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import net.bootsfaces.beans.BsfBeanInfo;

public class GrowlBeanInfo extends BsfBeanInfo {

	public PropertyDescriptor[] getCustomPropertyDescriptor() 
	throws IntrospectionException {
		PropertyDescriptor[] items = new PropertyDescriptor[] {
			new PropertyDescriptor("style-class", Growl.class, "getStyleClass", "setStyleClass") {{ setBound(true); }},
			new PropertyDescriptor("placement-from", Growl.class, "getPlacementFrom", "setPlacementFrom") {{ setBound(true); }},
			new PropertyDescriptor("placement-align", Growl.class, "getPlacementAlign", "setPlacementAlign") {{ setBound(true); }},
			new PropertyDescriptor("newest-on-top", Growl.class, "isNewestOnTop", "setNewestOnTop") {{ setBound(true); }},
			new PropertyDescriptor("allow-dismiss", Growl.class, "isAllowDismiss", "setAllowDismiss") {{ setBound(true); }},
			new PropertyDescriptor("global-only", Growl.class, "isGlobalOnly", "setGlobalOnly") {{ setBound(true); }},
			new PropertyDescriptor("show-detail", Growl.class, "isShowDetail", "setShowDetail") {{ setBound(true); }},
			new PropertyDescriptor("show-summary", Growl.class, "isShowSummary", "setShowSummary") {{ setBound(true); }}
		};
		
		return items;
	}
}
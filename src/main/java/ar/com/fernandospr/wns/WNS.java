package ar.com.fernandospr.wns;

/**
 * The main class to interact with the WNS Service.
 *
 */
public class WNS {

    private WNS() { throw new AssertionError("Uninstantiable class"); }

    
    public static WnsService newService() {
        return new WnsService();
    }

    
}

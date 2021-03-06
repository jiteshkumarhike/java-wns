package ar.com.fernandospr.wns.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ar.com.fernandospr.wns.WnsProxyProperties;
import ar.com.fernandospr.wns.exceptions.WnsException;
import ar.com.fernandospr.wns.model.*;
import ar.com.fernandospr.wns.model.types.WnsNotificationType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;


public class WnsClient {
	private static final String SCOPE = "notify.windows.com";
	private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
	private static final String AUTHENTICATION_URI = "https://login.live.com/accesstoken.srf";
	
	private String sid;
	private String clientSecret;
	private WnsOAuthToken token;
	private Client client;
	private ExecutorService executorService;
	
	public WnsClient(String sid, String clientSecret, boolean logging) {
		this.sid = sid;
		this.clientSecret = clientSecret;
		this.client = createClient(logging);
	}
	
	public WnsClient(String sid, String clientSecret, boolean logging , int maxConnections, ExecutorService executor) {
		this.sid = sid;
		this.clientSecret = clientSecret;
		this.client = createClient(logging, maxConnections);
		this.executorService = executor;
	}
	
	public WnsClient(String sid, String clientSecret, WnsProxyProperties proxyProps, boolean logging) {
		this.sid = sid;
		this.clientSecret = clientSecret;
		this.client = createClient(logging, proxyProps);
	}
	
	private static Client createClient(boolean logging) {
        ClientConfig clientConfig = new ClientConfig(JacksonJaxbXMLProvider.class, JacksonJsonProvider.class);
        Client client = ClientBuilder.newClient(clientConfig);

        if (logging) {
            LoggingFilter loggingFilter = new LoggingFilter(
                    Logger.getLogger(WnsClient.class.getName()), true);

            client = client.register(loggingFilter);
        }
        return client;
    }

	private static Client createClient(boolean logging, int maxConnections) {
        ClientConfig clientConfig = new ClientConfig(JacksonJaxbXMLProvider.class, JacksonJsonProvider.class);
        Client client = ClientBuilder.newClient(clientConfig);
        //if(maxConnections > 1)
			//clientConfig.getProperties().put(ClientProperties.ASYNC_THREADPOOL_SIZE,maxConnections);
        if (logging) {
            LoggingFilter loggingFilter = new LoggingFilter(
                    Logger.getLogger(WnsClient.class.getName()), true);

            client = client.register(loggingFilter);
        }
        return client;
    }
	
    private static Client createClient(boolean logging, WnsProxyProperties proxyProps) {
        ClientConfig clientConfig = new ClientConfig(JacksonJaxbXMLProvider.class, JacksonJsonProvider.class)
                .connectorProvider(new ApacheConnectorProvider());
        setProxyCredentials(clientConfig, proxyProps);

        Client client = ClientBuilder.newClient(clientConfig);
        if (logging) {
            LoggingFilter loggingFilter = new LoggingFilter(
                    Logger.getLogger(WnsClient.class.getName()), true);

            client = client.register(loggingFilter);
        }
        return client;
    }
	
	private static void setProxyCredentials(ClientConfig clientConfig, WnsProxyProperties proxyProps) {
        if (proxyProps != null) {
            String proxyProtocol = proxyProps.getProtocol();
            String proxyHost = proxyProps.getHost();
            int proxyPort = proxyProps.getPort();
            String proxyUser = proxyProps.getUser();
            String proxyPass = proxyProps.getPass();

            if ((proxyHost != null) && (!proxyHost.trim().isEmpty())) {
                clientConfig.property(ClientProperties.PROXY_URI, proxyProtocol + "://" + proxyHost + ":" + proxyPort);
                if (!proxyUser.trim().isEmpty()) {
                    clientConfig.property(ClientProperties.PROXY_PASSWORD, proxyPass);
                    clientConfig.property(ClientProperties.PROXY_USERNAME, proxyUser);
                }
            }

        }

    }


    public void refreshAccessToken() throws WnsException {
        WebTarget target = client.target(AUTHENTICATION_URI);

        MultivaluedStringMap formData = new MultivaluedStringMap();
        formData.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        formData.add("client_id", this.sid);
        formData.add("client_secret", this.clientSecret);
        formData.add("scope", SCOPE);
        Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.form(formData));

        if (response.getStatus() != 200) {
            throw new WnsException("Authentication failed. HTTP error code: " + response.getStatus());
        }

        this.token = response.readEntity(WnsOAuthToken.class);
    }
	
	public WnsNotificationResponse push(final WnsResourceBuilder resourceBuilder, final String channelUri, final WnsAbstractNotification notification, int retriesLeft, final WnsNotificationRequestOptional optional) throws WnsException {
		@SuppressWarnings("unchecked")
		Future future = executorService.submit(new Callable(){
		    public Object call() throws Exception {
		    	WebTarget target = client.target(channelUri);
		        Invocation.Builder webResourceBuilder = resourceBuilder.build(target, notification, getToken().access_token, optional);
		        String type = notification.getType().equals(WnsNotificationType.RAW) ? MediaType.APPLICATION_OCTET_STREAM : MediaType.TEXT_XML;

		        Response response = webResourceBuilder.buildPost(Entity.entity(resourceBuilder.getEntityToSendWithNotification(notification), type)).invoke();

		        WnsNotificationResponse notificationResponse = new WnsNotificationResponse(channelUri, response.getStatus(), response.getStringHeaders()    );
				return notificationResponse;
				
		    }
		});
		WnsNotificationResponse notificationResponse = null;
		try {
			notificationResponse = (WnsNotificationResponse)future.get();
//			System.out.println("future.get() = " + notificationResponse);
			if(notificationResponse.code == 200)
				return notificationResponse;
			if (notificationResponse.code == 401 && retriesLeft > 0) {
	 			retriesLeft--;
	 			// Access token may have expired
	 			refreshAccessToken();
	 			// Retry
	 			return this.push(resourceBuilder, channelUri, notification, retriesLeft, optional);
	 		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// Assuming push failed
		return notificationResponse;
	}
	
	private WnsOAuthToken getToken() throws WnsException {
		if (this.token == null) {
			refreshAccessToken();
		}
		return this.token;
	}

	public List<WnsNotificationResponse> push(WnsResourceBuilder resourceBuilder, List<String> channelUris, WnsAbstractNotification notification, int retriesLeft, WnsNotificationRequestOptional optional) throws WnsException {
		List<WnsNotificationResponse> responses = new ArrayList<WnsNotificationResponse>();
		for (String channelUri : channelUris) {
			WnsNotificationResponse response = push(resourceBuilder, channelUri, notification, retriesLeft, optional);
			responses.add(response);
		}
		return responses;
	}
	
}

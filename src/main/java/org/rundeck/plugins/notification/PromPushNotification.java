package org.rundeck.plugins.notification;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;

@Plugin(service = "Notification", name = "PromPushNotification")
@PluginDescription(title = "Prometheus Push Notification", description = "Send job notifications to Prometheus Pushgateway.")
public class PromPushNotification implements NotificationPlugin {
    @PluginProperty(name = "pushGwUrl", title = "Prometheus Pushgateway URL", description = "Prometheus Pushgateway URL", required = true)
    private String pushGwUrl;

    @PluginProperty(name = "pushGwJobName", title = "Job Name", description = "Job name", defaultValue = "rundeck")
    private String pushGwJobName;

    private static final Logger LOG = LoggerFactory.getLogger(PromPushNotification.class);

    private static String UrlBuilder(String url, String path) throws URISyntaxException {
        URIBuilder ub = new URIBuilder(url);
        ub.setPath(path);

        return ub.toString();
    }

    private static String UrlPathBuilder(String job, Map executionData) {
        String rdProjectName;
        String rdGroupName;
        String rdJobName;
        Map rdJobData;
        Object rdJob;

        rdProjectName = executionData.get("project").toString();

        rdJob = executionData.get("job");
        rdJobData = (Map) rdJob;

        if (!rdJobData.containsKey("group") || rdJobData.get("group").toString().equals("")) {
            rdGroupName = "root";
        } else {
            rdGroupName = rdJobData.get("group").toString();
        }

        rdJobName = rdJobData.get("name").toString();

        return "/metrics/job/" + job.replace("/", "-") + "/project_name/" + rdProjectName.replace("/", "-") + "/group_name/" + rdGroupName.replace("/", "-") + "/job_name/" + rdJobName.replace("/", "-");
    }


    private static void HttpRequest(String url, String body) {
        // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-jvm-ttl.html
        HttpClient client = new HttpClient();

        PostMethod method = new PostMethod(url);
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        method.setRequestHeader("Accept", "Accept: */*");
        method.setRequestEntity(new StringRequestEntity(body));

        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        try {
            int statusCode = client.executeMethod(method);

            if (statusCode != 200) {
                LOG.error("Method failed: " + method.getStatusLine());
            } else {
                LOG.info("Method success: " + method.getStatusLine());
            }

            byte[] responseBody = method.getResponseBody();

            LOG.info(responseBody.toString());

        } catch (HttpException e) {
            LOG.error("Fatal protocol violation: " + e.getMessage());
        } catch (IOException e) {
            LOG.error("Fatal transport error: " + e.getMessage());
        } finally {
            method.releaseConnection();
        }
    }

    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {
        String path = UrlPathBuilder(pushGwJobName, executionData);
        String payload = buildPayload(trigger, executionData);
        String URI;

        try {
            URI = UrlBuilder(pushGwUrl, path);
        } catch (URISyntaxException e) {
            LOG.error("Cannot build URL: " + e.getMessage());
            return false;
        }

        HttpRequest(URI, payload);

        return true;
    }

    private static String buildPayload(String trigger, Map executionData) {

        if (!executionData.containsKey("dateEndedUnixtime") || executionData.get("dateEndedUnixtime").toString() == "") {
            return "";
        }

        Double dateEndedUnixtime = Double.parseDouble(executionData.get("dateEndedUnixtime").toString());
        Double dateStartedUnixtime = Double.parseDouble(executionData.get("dateStartedUnixtime").toString());

        Double duration = (dateEndedUnixtime-dateStartedUnixtime)/1000;

        String metricName = "rundeck_" + trigger + "_job ";

        return "# TYPE " + metricName + "gauge\n" + metricName + duration.toString() + "\n";
    }
}

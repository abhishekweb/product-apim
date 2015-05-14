/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.mediation.SynapseConfigAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Configure a new handler and Invoke the API and verify  the  request is going through newly added handler.
 */
public class AddNewHandlerAndInvokeAPI extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI";
    private static final String API_CONTEXT = "testAPI";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "AddNewHandlerAndInvokeAPI";
    private final static String RESPONSE_GET = "I was at CustomAPIAuthenticationHandler";
    private static final String EXPECTED_HANDLER_LOG_OUTPUT =
            "I am at CustomAPIAuthenticationHandler:CustomAuthKey 123456789";
    private final static String API_GET_ENDPOINT_METHOD = "/handler";
    private final static String CUSTOM_AUTHORIZATION = "CustomAuthKey 123456789";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String accessToken;
    private HashMap<String, String> requestHeadersGet;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private String webAppTargetPath;
    private String customHandlerTargetPath;
    private String synapseConfigArtifactsPath;
    private String originalSynapseConfig;
    private String newSynapseConfig;
    private APIIdentifier apiIdentifier;
    private SynapseConfigAdminClient synapseConfigAdminClient;
    private String gatewaySession;
    private String apiEndPointUrl;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        synapseConfigArtifactsPath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "lifecycletest" + File.separator + "synapseconfig.xml";
        newSynapseConfig = readFile(synapseConfigArtifactsPath);
        String webAppSourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";
        webAppTargetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator +
                        "deployment" + File.separator + "server" + File.separator + "webapps";
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        FileManager.copyResourceToFileSystem(webAppSourcePath, webAppTargetPath, "jaxrs_basic.war");
        String customHandlerSourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "lifecycletest" + File.separator + "CustomAPIAuthenticationHandler-1.0.0.jar";
        customHandlerTargetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator +
                        "components" + File.separator + "lib";
        FileManager.copyResourceToFileSystem(customHandlerSourcePath, customHandlerTargetPath,
                "CustomAPIAuthenticationHandler-1.0.0.jar");
        String log4jPropertiesFile =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "lifecycletest" + File.separator + "log4j.properties";
        String log4jPropertiesTargetLocation =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator +
                        "conf" + File.separator + "log4j.properties";
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(log4jPropertiesFile), new File(log4jPropertiesTargetLocation), true);
        serverConfigurationManager.restartGracefully();
        super.init();
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        gatewaySession = createSession(gatewayContext);
        synapseConfigAdminClient =
                new SynapseConfigAdminClient(gatewayUrls.getWebAppURLHttps() + "services/", gatewaySession);
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the APi and check the  API request is going through the new handler.")
    public void testAPIInvocationHitsTheNewHandler() throws APIManagerIntegrationTestException, IOException,
            XMLStreamException, LogViewerLogViewerException {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        originalSynapseConfig = synapseConfigAdminClient.getConfiguration();
        synapseConfigAdminClient.updateConfiguration(newSynapseConfig);
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("Content-Type", "text/plain");
        //get the  access token
        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        requestHeadersGet.put("CustomAuthorization", CUSTOM_AUTHORIZATION);
        LogViewerClient logViewerClient =
                new LogViewerClient(gatewayUrls.getWebAppURLHttps() + "services/", gatewaySession);
        logViewerClient.clearLogs();
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_GET_ENDPOINT_METHOD, requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponse.getData() + "\"");
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        boolean isNewHandlerCalled = false;
        for (LogEvent logEvent : logEvents) {
            if (logEvent.getMessage().contains(EXPECTED_HANDLER_LOG_OUTPUT)) {
                isNewHandlerCalled = true;
                break;
            }
        }
        assertTrue(isNewHandlerCalled, "API Request not went through the new handler");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, XMLStreamException,
            RemoteException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
        //Restore original synapse configuration
        synapseConfigAdminClient.updateConfiguration(originalSynapseConfig);
    }


}

package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.manager.CustomerIdResolver;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class BridgesAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    CustomerIdResolver customerIdResolver;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void testGetEmptyBridges() {
        BridgeListResponse response = TestUtils.getBridges().as(BridgeListResponse.class);
        Assertions.assertEquals(0, response.getItems().size());
    }

    @Test
    public void createBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
    }

    @Test
    public void getBridge() {
        Response bridgeCreateResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        bridgeCreateResponse.then().statusCode(201);

        BridgeResponse bridge = bridgeCreateResponse.as(BridgeResponse.class);

        BridgeResponse retrievedBridge = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        Assertions.assertNotNull(retrievedBridge);
        Assertions.assertEquals(bridge.getId(), retrievedBridge.getId());
        Assertions.assertEquals(bridge.getName(), retrievedBridge.getName());
        Assertions.assertEquals(bridge.getEndpoint(), retrievedBridge.getEndpoint());
    }

    @Test
    public void getUnexistingBridge() {
        TestUtils.getBridge("not-the-id").then().statusCode(404);
    }

    @Test
    public void testCreateAndGetBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);

        BridgeListResponse bridgeListResponse = TestUtils.getBridges().as(BridgeListResponse.class);

        Assertions.assertEquals(1, bridgeListResponse.getItems().size());
        BridgeResponse bridgeResponse = bridgeListResponse.getItems().get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridgeResponse.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridgeResponse.getStatus());
        Assertions.assertEquals(APIConstants.USER_API_BASE_PATH + bridgeResponse.getId(), bridgeResponse.getHref());
        Assertions.assertNotNull(bridgeResponse.getSubmittedAt());

        Assertions.assertNull(bridgeResponse.getEndpoint());
    }

    @Test
    public void testDeleteBridge() {
        BridgeResponse response = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        TestUtils.deleteBridge(response.getId()).then().statusCode(202);
        response = TestUtils.getBridge(response.getId()).as(BridgeResponse.class);

        Assertions.assertEquals(BridgeStatus.DELETION_REQUESTED, response.getStatus());
    }

    @Test
    public void testAlreadyExistingBridge() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(201);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME))
                .then().statusCode(400);
    }

    @Test
    public void addProcessorToBridge() {

        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        BridgeResponse bridgeResponse = TestUtils.createBridge(r).as(BridgeResponse.class);

        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridgeResponse.getId());
        dto.setStatus(BridgeStatus.AVAILABLE);
        dto.setCustomerId(customerIdResolver.resolveCustomerId());
        dto.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(dto);
        assertThat(deployment.getStatusCode(), equalTo(200));

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName(), equalTo("myProcessor"));
        assertThat(processorResponse.getBridge().getId(), equalTo(bridgeResponse.getId()));
    }

    @Test
    public void addProcessorToBridge_bridgeDoesNotExist() {

        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge_bridgeNotInAvailableStatus() {

        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode(), equalTo(400));
    }
}
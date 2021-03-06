package fr.xebia.xebay.api.rest;

import fr.xebia.xebay.api.RegisterRule;
import fr.xebia.xebay.api.dto.BidDemand;
import fr.xebia.xebay.domain.BidEngine;
import fr.xebia.xebay.domain.BidOffer;
import fr.xebia.xebay.domain.Item;
import fr.xebia.xebay.domain.PluginInfo;
import fr.xebia.xebay.domain.internal.AdminUser;
import fr.xebia.xebay.utils.TomcatRule;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.NoSuchElementException;
import java.util.Set;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class BidEngineResourceIT {
    @ClassRule
    public static TomcatRule tomcatRule = new TomcatRule();
    @Rule
    public RegisterRule registerRule = new RegisterRule();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Client client;
    private WebTarget target;

    @Before
    public void initializeRestClient() throws Exception {
        client = ClientBuilder.newBuilder().register(JacksonFeature.class).property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).build();
        target = client.target("http://localhost:8080/rest/bidEngine");
    }

    @After
    public void tearDown() throws Exception {
        target = null;
        client.close();
    }

    @Test
    public void current_bidOffer_can_be_retrieved() throws Exception {
        BidOffer bidOffer = target.path("current").request().get(BidOffer.class);
        assertThat(bidOffer.getItem().getName()).isEqualTo("an item");
        assertThat(bidOffer.getItem().getCategory()).isEqualTo("category");
        assertThat(bidOffer.getItem().getValue()).isNotNull();
        assertThat(bidOffer.getTimeToLive()).isLessThan(BidEngine.DEFAULT_TIME_TO_LIVE).isNotNegative();
        assertThat(bidOffer.getBidder()).isNull();
        assertThat(bidOffer.getItem().isOffered()).isFalse();
    }

    @Test
    public void cant_bid_if_user_not_registered() {
        String fakeKey = "fake key";

        BidOffer currentBidOffer = target.path("current").request().get(BidOffer.class);
        Form form = new Form();
        form.param("name", currentBidOffer.getItem().getName());
        form.param("value", String.valueOf(currentBidOffer.getItem().getValue()));
        form.param("increment", "0.7");

        expectedException.expect(NotAuthorizedException.class);
        expectedException.expectMessage("HTTP 401 Unauthorized");

        target.path("/bid").request()
                .header(HttpHeaders.AUTHORIZATION, fakeKey)
                .post(Entity.entity(form, MediaType.APPLICATION_JSON_TYPE), BidOffer.class);
    }

    @Test
    public void a_registered_user_can_bid() throws Exception {
        BidOffer currentBidOffer = target.path("current").request().get(BidOffer.class);
        double firstValue = currentBidOffer.getItem().getValue();

        BidDemand bidDemand = new BidDemand(currentBidOffer.getItem().getName(), currentBidOffer.getItem().getValue() + 0.7);

        BidOffer bidOffer = target.path("/bid").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .post(Entity.entity(bidDemand, MediaType.APPLICATION_JSON_TYPE), BidOffer.class);
        assertThat(bidOffer.getItem().getName()).isEqualTo("an item");
        assertThat(bidOffer.getItem().getValue()).isEqualTo(firstValue + 0.7);
        assertThat(bidOffer.getTimeToLive()).isLessThan(BidEngine.DEFAULT_TIME_TO_LIVE).isNotNegative();
    }

    @Test
    public void should_throw_forbidden_exception_when_user_try_offering_item_not_belonging_to_him() throws Exception {
        BidDemand bidDemand = new BidDemand("an item", 10.0);

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("HTTP 403 Forbidden");
        target.path("/offer").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .post(Entity.entity(bidDemand, MediaType.APPLICATION_JSON_TYPE), Item.class);
    }

    @Test
    public void should_throw_notfound_exception_when_user_offering_item_that_doesnt_exists() throws Exception {
        BidDemand bidDemand = new BidDemand("unknown item", 10.0);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("HTTP 404 Not Found");

        target.path("/offer").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .post(Entity.entity(bidDemand, MediaType.APPLICATION_JSON_TYPE), Item.class);
    }

    @Test
    @Ignore("api should throw 400 and not 500 server error if empty body sent (upgrade jackson and JsonMappingExceptionMapper to throw 400)")
    public void should_throw_bad_request_exception_when_posting_null() throws Exception {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("HTTP 400 Bad Request");

        target.path("/offer").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE), Item.class);
    }

    @Test(expected = ForbiddenException.class)
    public void user_can_not_change_plugin_status() {
        target.path("plugin").path("BankBuyEverything").path("activate").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .method("PATCH", Void.TYPE);
    }

    @Test
    public void admin_can_change_plugin_status() {
        try {
            target.path("plugin").path("BankBuyEverything").path("activate").request()
                    .header(HttpHeaders.AUTHORIZATION, AdminUser.KEY)
                    .method("PATCH", Void.TYPE);

            Set<PluginInfo> pluginInfos = target.path("news").request()
                    .header(HttpHeaders.AUTHORIZATION, AdminUser.KEY)
                    .get(new GenericType<Set<PluginInfo>>() {
                    });

            assertThat(getPluginByName(pluginInfos, "BankBuyEverything").isActivated()).isTrue();
        } finally {
            target.path("plugin").path("BankBuyEverything").path("deactivate").request()
                    .header(HttpHeaders.AUTHORIZATION, AdminUser.KEY)
                    .method("PATCH", Void.TYPE);
        }
    }

    @Test
    public void user_can_retrieve_news() {
        Set<PluginInfo> pluginInfos = target.path("news").request()
                .header(HttpHeaders.AUTHORIZATION, registerRule.getKey())
                .get(new GenericType<Set<PluginInfo>>() {
                });
        assertThat(pluginInfos).hasSize(2);
        assertThat(pluginInfos)
                .extracting("name", "description", "activated").containsOnly(
                tuple("BankBuyEverything", "Bank buy every item sold at his cost price !", false),
                tuple("AllItemsInCategory", "Bonus to user having all items in category", false));
    }

    private PluginInfo getPluginByName(Set<PluginInfo> plugins, String pluginName) {
        for (PluginInfo plugin : plugins) {
            if (plugin.getName().equals(pluginName)) {
                return plugin;
            }
        }
        throw new NoSuchElementException(format("plugin %s was not found", pluginName));
    }
}

package bid.api.rest;

import bid.api.rest.dto.BidOfferInfo;
import bid.api.rest.security.SecurityContextImpl;
import bid.domain.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationTest {
    @Rule
    public ExpectedException excpectedException = ExpectedException.none();

    private Items items;

    @Before
    public void createBidOffer() {
        items = new Items(new Item("an item", 4.3));
    }

    private String register() {
        UserResource userResource = new UserResource(new Users());
        return userResource.register("an-email@provider.com");
    }


    @Test
    public void a_key_is_emmitted_when_register() {
        UserResource userResource = new UserResource(new Users());
        String key = userResource.register("an-email@provider.com");

        assertThat(key).matches(Pattern.compile("[\\w\\-_]+")).hasSize(16);
    }

    @Test
    public void can_not_register_twice() {
        UserResource userResource = new UserResource(new Users());
        userResource.register("an-email@provider.com");
        excpectedException.expect(BidException.class);
        excpectedException.expectMessage("\"an-email@provider.com\" is already registered");

        userResource.register("an-email@provider.com");
    }



    @Test
    public void cant_get_user_if_key_is_not_correct() {
        UserResource userResource = new UserResource(new Users());

        excpectedException.expect(UserNotAllowedException.class);
        excpectedException.expectMessage("key \"fake key\" is unknown");

        userResource.getUser("fake key");
    }

    @Test
    public void cant_bid_if_no_user() {
        BidEngine bidEngine = new BidEngine(items);
        excpectedException.expect(BidException.class);
        excpectedException.expectMessage("bad user");

        BidEngineResource bidEngineResource = new BidEngineResource();
        BidOfferInfo bidOffer = bidEngineResource.currentBidOffer();

        bidEngineResource.bid("an item", 4.3, 20.0, new SecurityContextImpl(null)); //todo fake user without right
    }

    @Test
    public void a_user_can_interact_with_server() {
//        String key = register();

        BidEngineResource bidEngineResource = new BidEngineResource();
        BidOfferInfo bidOffer = bidEngineResource.currentBidOffer();

        bidEngineResource.bid(bidOffer.getItemName(), bidOffer.getCurrentValue(), 20.0, new SecurityContextImpl(new User("abc", "an-email@provider.com")));
    }

}
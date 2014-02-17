package fr.xebia.xebay;

import fr.xebia.xebay.domain.BidEngine;
import fr.xebia.xebay.domain.Item;
import fr.xebia.xebay.domain.Items;
import fr.xebia.xebay.domain.Users;

public enum BidServer {
    BID_SERVER;

    public final Users users;
    public final Items items;
    public final BidEngine bidEngine;

    BidServer() {
        users = new Users();
        if (System.getProperty("xebay.test") != null) {
            items = new Items(new Item("category", "an item", 4.3));
            bidEngine = new BidEngine(items, () -> false);
            return;
        }

        items = Items.load("items").get();
        bidEngine = new BidEngine(items);

    }
}

package fr.xebia.xebay.domain;

import fr.xebia.xebay.domain.internal.*;
import fr.xebia.xebay.domain.internal.BidOffer;
import fr.xebia.xebay.domain.internal.Item;
import fr.xebia.xebay.domain.internal.User;
import fr.xebia.xebay.domain.plugin.Plugins;

import java.util.*;

import static java.lang.String.format;

public class BidEngine {
    public static final int DEFAULT_TIME_TO_LIVE = 10000;

    private final List<BidEngineListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final Items items;
    private final Expirable bidOfferExpiration;
    private final Deque<BidOfferToSell> bidOffersToSell;
    private final Plugins plugins;

    private Optional<BidOffer> bidOffer;

    public BidEngine(Items items) {
        this.items = items;
        this.bidOfferExpiration = () -> !bidOffer.isPresent() || bidOffer.get().getTimeToLive() == 0;
        this.bidOffersToSell = new ArrayDeque<>();
        this.bidOffer = Optional.of(new BidOffer(this.items.next(), DEFAULT_TIME_TO_LIVE));
        this.plugins = new Plugins();
    }

    public BidEngine(Items items, Expirable bidOfferExpiration) {
        this.items = items;
        this.bidOfferExpiration = bidOfferExpiration;
        this.bidOffersToSell = new ArrayDeque<>();
        this.bidOffer = Optional.of(new BidOffer(this.items.next(), DEFAULT_TIME_TO_LIVE));
        this.plugins = new Plugins();
    }

    public fr.xebia.xebay.domain.BidOffer currentBidOffer() {
        nextBidOfferIfExpired();
        return bidOffer.isPresent() ? bidOffer.get().toBidOffer() : null;
    }

    public fr.xebia.xebay.domain.BidOffer bid(User user, String itemName, double newValue) throws BidException {
        if (user.isInRole(AdminUser.ADMIN_ROLE)) {
            throw new BidException("admin is not authorized to bid");
        }
        nextBidOfferIfExpired();
        fr.xebia.xebay.domain.BidOffer updatedBidOffer = bidOffer
                .orElseThrow(() -> new BidException(format("current item to bid is not \"%s\"", itemName)))
                .bid(itemName, new Amount(newValue), user)
                .toBidOffer();
        notifyListeners(updatedBidOffer);
        return updatedBidOffer;
    }

    public void offer(User user, Item item, double initialValue) {
        nextBidOfferIfExpired();
        checkUserOffer(user, item);
        BidOfferToSell bidOfferToSell = checkOffer(item, new Amount(initialValue));
        if (plugins.authorize(bidOfferToSell)) {
            item.setOffered();
            bidOffersToSell.offerLast(bidOfferToSell);
        }
    }

    private BidOfferToSell checkOffer(Item item, Amount initialValue) {
        if (bidOffer.isPresent() && bidOffer.get().item.equals(item)) {
            throw new BidForbiddenException(format("item \"%s\" is the current offer thus can't be offered", item.getName()));
        }
        BidOfferToSell bidOfferToSell = new BidOfferToSell(item, initialValue);
        if (bidOffersToSell.contains(bidOfferToSell)) {
            throw new BidForbiddenException(format("item \"%s\" is already offered", item));
        }
        return bidOfferToSell;
    }

    private void checkUserOffer(User user, Item item) {
        if (!user.equals(item.getOwner())) {
            throw new BidForbiddenException(format("item \"%s\" doesn't belong to user \"%s\"", item, user));
        }
    }

    public void userIsUnregistered(User user) {
        bidOffer.ifPresent((bidOffer) -> bidOffer.userIsUnregistered(user));
        items.userIsUnregistered(user);
    }

    private synchronized void nextBidOfferIfExpired() {
        if (bidOfferExpiration.isExpired()) {
            bidOffer.ifPresent((bidOffer) -> {
                bidOffer.resolve();
                plugins.onBidOfferResolved(bidOffer, items);
                notifyListeners(bidOffer.toBidOffer());
            });
            bidOffer = nextBidOffer();
            bidOffer.ifPresent((bidOffer) -> notifyListeners(bidOffer.toBidOffer()));
        }
    }

    private Optional<BidOffer> nextBidOffer() {

        // first, look for items offered by users
        if (!bidOffersToSell.isEmpty()) {
            return Optional.of(bidOffersToSell.pollFirst().toBidOffer(DEFAULT_TIME_TO_LIVE));
        }

        // then, look for items offered by bank
        Item nextItem = items.next();
        return nextItem == null ? Optional.empty() : Optional.of(new BidOffer(nextItem, DEFAULT_TIME_TO_LIVE));
    }

    public Set<PluginInfo> getPlugins() {
        return plugins.toPluginSet();
    }

    public void activate(String pluginName) {
        plugins.activate(pluginName, items);
        notifyListeners(plugins.getPluginInfo(pluginName));
    }

    public void deactivate(String pluginName) {
        plugins.deactivate(pluginName);
        notifyListeners(plugins.getPluginInfo(pluginName));
    }

    public void addListener(BidEngineListener bidEngineListener) {
        nextBidOfferIfExpired();
        synchronized (listeners) {
            listeners.add(bidEngineListener);
        }
    }

    private void notifyListeners(PluginInfo info) {
        synchronized (listeners) {
            listeners.forEach(bidEngineListener -> bidEngineListener.onNews(info));
        }
    }

    private void notifyListeners(fr.xebia.xebay.domain.BidOffer bidOffer) {
        synchronized (listeners) {
            listeners.forEach(bidEngineListener -> bidEngineListener.onBid(bidOffer));
        }
    }
}

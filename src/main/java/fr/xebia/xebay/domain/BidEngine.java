package fr.xebia.xebay.domain;

import fr.xebia.xebay.domain.internal.*;
import fr.xebia.xebay.domain.internal.Item;
import fr.xebia.xebay.domain.internal.User;

import java.util.*;

import static java.lang.String.format;

public class BidEngine {
    public static final int DEFAULT_TIME_TO_LIVE = 10000;

    private final List<BidEngineListener> listeners = new ArrayList<>();
    private final Items items;
    private final Expirable bidOfferExpiration;
    private final Queue<BidOfferToSell> bidOffersToSell;

    private int timeToLive;
    private Optional<MutableBidOffer> bidOffer;

    public BidEngine(Items items) {
        this.items = items;
        this.bidOfferExpiration = () -> !bidOffer.isPresent() || bidOffer.get().isExpired();
        this.bidOffersToSell = new ArrayDeque<>();
        this.timeToLive = DEFAULT_TIME_TO_LIVE;
        this.bidOffer = Optional.of(new MutableBidOffer(this.items.next(), timeToLive));
    }

    public BidEngine(Items items, Expirable bidOfferExpiration) {
        this.items = items;
        this.bidOfferExpiration = bidOfferExpiration;
        this.bidOffersToSell = new ArrayDeque<>();
        this.timeToLive = DEFAULT_TIME_TO_LIVE;
        this.bidOffer = Optional.of(new MutableBidOffer(this.items.next(), timeToLive));
    }

    public BidOffer currentBidOffer() {
        nextBidOfferIfExpired();
        return bidOffer.isPresent() ? bidOffer.get().toBidOffer(bidOfferExpiration.isExpired()) : null;
    }

    public BidOffer bid(User user, String itemName, double newValue) throws BidException {
        if (user.isInRole(AdminUser.ADMIN_ROLE)) {
            throw new BidException("admin is not authorized to bid");
        }
        nextBidOfferIfExpired();
        BidOffer updatedBidOffer = bidOffer.orElseThrow(() -> new BidException(format("current item to bid is not \"%s\"", itemName)))
                .bid(itemName, newValue, user)
                .toBidOffer(bidOfferExpiration.isExpired());
        listeners.forEach(bidEngineListener -> bidEngineListener.onBidOfferUpdated(updatedBidOffer));
        return updatedBidOffer;
    }

    public void offer(User user, Item item, double initialValue) {
        nextBidOfferIfExpired();
        checkUserOffer(user, item);
        BidOfferToSell bidOfferToSell = checkOffer(item, initialValue);
        bidOffersToSell.offer(bidOfferToSell);
    }

    private BidOfferToSell checkOffer(Item item, double initialValue) {
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


    public void addListener(BidEngineListener bidEngineListener) {
        nextBidOfferIfExpired();
        listeners.add(bidEngineListener);
    }

    public void userIsUnregistered(User user) {
        bidOffer.ifPresent((bidOffer) -> bidOffer.userIsUnregistered(user));
        items.userIsUnregistered(user);
    }

    private void nextBidOfferIfExpired() {
        if (bidOfferExpiration.isExpired()) {
            bidOffer.ifPresent((bidOffer) -> {
                bidOffer.resolve();
                listeners.forEach(bidEngineListener -> bidEngineListener.onBidOfferResolved(bidOffer.toBidOffer(true)));
            });
            bidOffer = nextBidOffer();
            bidOffer.ifPresent((bidOffer) -> listeners.forEach(bidEngineListener -> bidEngineListener.onBidOfferStarted(bidOffer.toBidOffer(true))));
        }
    }

    private Optional<MutableBidOffer> nextBidOffer() {
        if (!bidOffersToSell.isEmpty()) {
            return Optional.of(bidOffersToSell.poll().toBidOffer(timeToLive));
        }
        Item nextItem = items.next();
        if (nextItem == null) {
            return Optional.empty();
        } else {
            return Optional.of(new MutableBidOffer(nextItem, timeToLive));
        }
    }
}

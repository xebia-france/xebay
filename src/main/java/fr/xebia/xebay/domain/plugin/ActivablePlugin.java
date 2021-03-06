package fr.xebia.xebay.domain.plugin;

import fr.xebia.xebay.domain.PluginInfo;
import fr.xebia.xebay.domain.internal.BidOffer;
import fr.xebia.xebay.domain.internal.BidOfferToSell;
import fr.xebia.xebay.domain.internal.Items;

public abstract class ActivablePlugin implements Plugin {
    private final String name;

    private boolean activated;

    private final String description;

    protected ActivablePlugin(String pluginName, String description) {
        this.name = pluginName;
        this.description = description;
        this.activated = false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void activate(Items items) {
        activated = true;
        onActivation(items);
    }

    public void deactivate() {
        activated = false;
    }

    public PluginInfo toPlugin() {
        return new PluginInfo(name, description, activated);
    }

    protected void onActivation(Items items) {
    }

    @Override
    public final void onBidOfferResolved(BidOffer bidOffer, Items items) {
        if (activated) {
            onBidOfferResolvedIfActivated(bidOffer, items);
        }
    }

    protected void onBidOfferResolvedIfActivated(BidOffer bidOffer, Items items) {
    }

    @Override
    public final boolean authorize(BidOfferToSell bidOfferToSell) {
        return !activated || authorizeIfActivated(bidOfferToSell);
    }

    protected boolean authorizeIfActivated(BidOfferToSell bidOfferToSell) {
        return true;
    }
}

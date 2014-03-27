## Bid

### How to start server

    $ mvn tomcat7:run

### Coding Style

You can use [editorconfig](http://editorconfig.org) to synchronize this project coding style.

### TODO

 - add client with main (branch)
 - rename BidOfferToSell => ItemOffer, BidDemand => BidOffer and BidOffer to OfferResponse (or any better idea because I think names are not relevant) ?
 - web : as a visitor, I should see graph of values of each items
 - persist users (name, key)
 - add description to plugin
 - new plugins

nice to have :

 - web : as a user I should see my collection changes as I acquire items (needs a browser refresh at the moment)
 - web : provide a user page to update informations (name, avatar, etc.)
 - game : how much items / how much "value" => n° de collection

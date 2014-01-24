var xebay = {
  "init": function () {
    $(".registered").hide();
    $.cookie.json = true;
    this.displayBidOffer();
    var cookie = $.cookie("xebay");
    if (cookie) {
      this.signinWith(cookie.email, cookie.key);
    }
  },
  "signup": function () {
    this.signupWith($("#email").val());
  },
  "signupWith": function (email) {
    $.get("/rest/users/register", {"email": email}, function (key) {
      xebay.signedin(email, key);
    });
  },
  "signin": function () {
    this.signinWith($("#email").val(), $("#key").val());
  },
  "signinWith": function (email, key) {
    $.ajax("/rest/users/info", {
      "headers": {"Authorization": key},
      "data": {"email": email},
      "success": function (user) {
        xebay.signedin(email, key);
      }
    });
  },
  "signout": function () {
    this.signedout();
  },
  "unregister": function () {
    $.get("/rest/users/unregister", {"email": $.cookie("xebay").email, "key": $.cookie("xebay").key}, function () {
      xebay.signedout();
    });
  },
  "signedin": function (email, key) {
    $("#email-display").text(email);
    $("#key-display").text(key);
    $(".registered").show();
    $(".unregistered").hide();
    $.cookie("xebay", {"email": email, "key": key});
    this.connect(key);
  },
  "signedout": function () {
    $("#key-display").text("");
    $(".unregistered").show();
    $(".registered").hide();
    $.removeCookie("xebay");
  },
  "displayBidOffer": function () {
    $.getJSON("/rest/bidEngine",function (currentBidOffer) {
      $("#current-bid-offer").html("" +
          "<p>" + currentBidOffer["itemName"] + "</p>" +
          "<p>current value: " + currentBidOffer["currentValue"] + " bid points</p>");
      setTimeout(xebay.displayBidOffer, currentBidOffer["timeToLive"]);
    }).fail(function () {
          $("#current-bid-offer").html("" +
              "<p>There is no bid offer.</p>");
    });
  },
  "connect": function (key) {
      this.socket = new WebSocket("ws://" + window.location.host + "/socket/auctioneer/" + key);
      this.socket.onmessage = this.listenBidOffers;
      this.socket.onopen = this.connected;
      this.socket.onclose = this.disconnected;
      this.socket.onerror = this.disconnected;
  },
  "connected" : function() {
    $(".connected").show();
    $(".disconnected").hide();
  },
  "disconnected" : function() {
    $(".connected").hide();
    $(".disconnected").show();
  },
  "sendOffer" : function (increment) {
      this.socket.send(JSON.stringify({
          itemName: "test",
          curValue: 3,
          increment: increment
      }));
  },
  "listenBidOffers": function (message) {
      var bidOffer = JSON.parse(message.data);
      console.log("received bidOffer: " + bidOffer);
  }
};

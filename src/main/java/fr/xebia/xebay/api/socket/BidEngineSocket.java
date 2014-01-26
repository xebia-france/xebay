package fr.xebia.xebay.api.socket;

import fr.xebia.xebay.api.socket.coder.BidCallDecoder;
import fr.xebia.xebay.api.socket.coder.BidAnswerEncoder;
import fr.xebia.xebay.api.socket.dto.BidAnswer;
import fr.xebia.xebay.api.socket.dto.BidCall;
import fr.xebia.xebay.domain.*;

import javax.websocket.EncodeException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.xebia.xebay.BidServer.BID_SERVER;

@ServerEndpoint(value = "/socket/bidEngine/{authToken}", decoders = BidCallDecoder.class, encoders = BidAnswerEncoder.class)
public class BidEngineSocket implements BidEngineListener {

  static final Logger log = Logger.getLogger("BidEngineSocket");

  final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

  {
    BID_SERVER.bidEngine.addListener(this);
  }

  @OnOpen
  public void onOpen(Session session) {
    sessions.add(session);
  }

  @OnMessage
  public void onMessage(Session session, @PathParam("authToken") String authToken, BidCall bidCall) throws IOException, EncodeException {

    try {

      User user = BID_SERVER.users.getUser(authToken);
      BID_SERVER.bidEngine.bid(user, bidCall.getItemName(), bidCall.getCurValue(), bidCall.getIncrement());

    } catch (UserNotAllowedException | BidException e) {
      BidAnswer bidAnswer = BidAnswer.newRejected(e.getMessage(), bidCall);
      session.getBasicRemote().sendObject(bidAnswer);
    }
  }

  @Override
  public void onBidOffer(BidOffer bidOffer) {

    BidAnswer bidAnswer = BidAnswer.newAccepted(bidOffer);
    sessions.stream().filter(session -> session.isOpen()).forEach(session -> {
      try {
        session.getBasicRemote().sendObject(bidAnswer);
      } catch (IOException | EncodeException e) {
        log.log(Level.SEVERE, "BidInfo notification in error", e);
      }
    });
  }
}
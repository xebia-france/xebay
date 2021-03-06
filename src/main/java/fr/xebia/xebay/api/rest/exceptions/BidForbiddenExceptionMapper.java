package fr.xebia.xebay.api.rest.exceptions;

import fr.xebia.xebay.domain.BidForbiddenException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BidForbiddenExceptionMapper implements ExceptionMapper<BidForbiddenException> {
    @Override
    public Response toResponse(BidForbiddenException e) {
            Response response = Response.status(Status.FORBIDDEN.getStatusCode()).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        return response;
        }
    }
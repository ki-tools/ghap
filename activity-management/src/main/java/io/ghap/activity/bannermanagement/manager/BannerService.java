package io.ghap.activity.bannermanagement.manager;


import io.ghap.activity.bannermanagement.model.ApiBanner;
import io.ghap.activity.bannermanagement.model.ApiCreateBanner;
import io.ghap.activity.bannermanagement.model.ApiUpdateBanner;
import io.ghap.activity.bannermanagement.model.DateApiBanner;
import io.ghap.activity.exception.ApplicationException;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;

import javax.ws.rs.*;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;

public interface BannerService {

    ApiBanner create(SecurityContext securityContext, ApiCreateBanner apiBanner) throws ApplicationException;
    List<? extends DateApiBanner> getAll() throws ApplicationException;

    void delete(UUID id) throws ApplicationException;
    DateApiBanner get(@PathParam("id") UUID id) throws ApplicationException;

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    DateApiBanner update(ApiUpdateBanner apiBanner) throws ApplicationException;

    @GET
    @Path("/current")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    List<? extends DateApiBanner> current() throws ApplicationException;
}

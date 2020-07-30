package io.ghap.banner.manager;


import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import io.ghap.banner.exception.ApplicationException;
import io.ghap.banner.model.ApiBanner;
import io.ghap.banner.model.ApiCreateBanner;
import io.ghap.banner.model.ApiUpdateBanner;
import io.ghap.banner.model.DateApiBanner;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;

import javax.ws.rs.*;
import javax.ws.rs.core.SecurityContext;
import java.util.Set;
import java.util.UUID;

public interface BannerService {

    ApiBanner create(SecurityContext securityContext, ApiCreateBanner apiBanner) throws ApplicationException;
    Set<? extends DateApiBanner> getAll() throws ApplicationException;

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
    DateApiBanner current() throws ApplicationException;
}

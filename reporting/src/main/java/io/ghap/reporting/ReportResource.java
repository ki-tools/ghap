package io.ghap.reporting;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.Responses;
import com.sun.jersey.api.client.ClientResponse;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.OauthUtils;
import io.ghap.oauth.PredicateType;
import io.ghap.reporting.data.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A mechanisim for creating and retrieving various reports.  Currently there are a handful of reports
 * that can be created USER, GROUP, PROGRAMS, GRANTS, USAGE
 */
@Singleton
@Path("Reporting")
@OAuth20
@AllowedScopes(scopes = {"GHAP Administrator", "Administrators"}, predicateType = PredicateType.OR)
public class ReportResource {
  private Logger logger = LoggerFactory.getLogger(ReportResource.class);

  @Inject
  private ReportFactory reportFactory;

  /**
   * Create a new Report of the requested type.
   * @param uuid The user unique id that is requesting this report
   * @param reportType The type of report to create
   * @return
   */
  @Path("/create/{uuid}/{reportType}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context SecurityContext securityContext, @PathParam("uuid") UUID uuid, @PathParam("reportType") ReportType reportType) {
    String accessToken = OauthUtils.getAccessToken(securityContext);
    String token;
    try {
      token = reportFactory.CreateReport(accessToken, uuid, reportType);
    } catch (Throwable e) {
      if (e instanceof WebApplicationException) {
        throw ((WebApplicationException) e);
      }
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    return Response.ok(token).build();
  }

  /**
   * Create a new Report of the requested type.
   * @param uuid_str The user unique id that is requesting this report
   * @param reportType The type of report to create
   * @param constraints Any constraints required by this report type, such as a date range.
   * <code> {
   *  [{
   *    "start":1447302803706
   *    "end":1447302819526
   *  }]
   * }</code>
   * @return
   */
  @Path("/constrainedcreate/{uuid}/{reportType}")
  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response create(@Context SecurityContext securityContext, @PathParam("uuid") String uuid_str,
                         @PathParam("reportType") String reportType,
                         Constraint[] constraints) {
    ReportType rType = ReportType.valueOf(reportType);
    UUID uuid = UUID.fromString(uuid_str);
    String token = null;
    try {
      String accessToken = OauthUtils.getAccessToken(securityContext);
      token = reportFactory.CreateReport(accessToken, uuid, rType, Arrays.asList(constraints));
    } catch (Throwable e) {
      if (e instanceof WebApplicationException) {
        throw ((WebApplicationException) e);
      }
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    return Response.ok(token).build();
  }

  /**
   * Get a list of the reports that the system is capable of generating. Returns an array of ReportDescriptors
   * which will describe the report being created.  Each ReportDescriptor has a type of report, a category name,
   * a type name (which is what should be presented to the user), and a list of constraint types (if any).  Currently
   * the only supported constraint type is DATE_RANGE.
   *
   * [{"type":"USER_STATUS","categoryName":"Auditing","typeName":"User Accounts","constraintTypes":[]},
   *  {"type":"GROUP_STATUS","categoryName":"Auditing","typeName":"Group Membership","constraintTypes":[]}]

   */
  @Path("/getavailablereports")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAvailableReports()
  {
    List<ReportType> available_reports = reportFactory.GetAvailableReports();
    return Response.ok(ReportDescriptorFactory.get().getDescriptors(available_reports)).build();
  }

  /**
   * Get a report identified by a token.  If a report is still in the process of being created a status of ACCEPTED will
   * be returned, if a user requests a token that is not available a NOT_FOUND status will be returned, if an error is
   * encounted a SERVER_ERROR will be returned.  Otherwise the reponse will be OK, and contain a Report entity.
   * @param token
   * @return
   */
  @Path("/getreport/{token}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("token") String token) {
    switch(reportFactory.Status(token)) {
      case RUNNING:
        return Response.status(ClientResponse.Status.ACCEPTED).entity("Not Ready").build();
      case ERROR:
        return Response.serverError().entity("Unexpected Error Encountered").build();
      case NOT_FOUND:
        return Response.status(Response.Status.NOT_FOUND).build();
      default:
        return Response.ok(reportFactory.GetReport(token)).build();
    }
  }

  /**
   * Get the status of a report identified by a token.  If a report is still in the process of being created a status of ACCEPTED will
   * be returned, if a user requests a token that is not available a NOT_FOUND status will be returned, if an error is
   * encounted a SERVER_ERROR will be returned.  Otherwise the reponse will be OK, and contain a Report entity.
   * @param token
   * @return
   */
  @Path("/getstatus/{token}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@PathParam("token") String token) {
    switch (reportFactory.Status(token)) {
      case RUNNING:
        return Response.status(ClientResponse.Status.ACCEPTED).entity("Not Ready").build();
      case ERROR:
        return Response.serverError().entity("Unexpected Error Encountered").build();
      case NOT_FOUND:
        return Response.status(Response.Status.NOT_FOUND).build();
      default:
        return Response.ok(reportFactory.Status(token)).build();
    }
  }

  /**
   * Get the statuses of a group of reports identified by an array of tokens.
   * @param tokens  The list of report tokens to retrieve the statuses of.
   * @return
   */
  @Path("/getstatuses")
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStatusForTokens(@QueryParam("tokens") final List<String> tokens) {
    return statusForTokens(tokens);
  }

  /**
   * Get the statuses of a group of reports identified by an array of tokens.
   * @param tokens  The list of report tokens to retrieve the statuses of.
   * @return
   */
  @Path("/getstatuses")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postStatusForTokens(final List<String> tokens) {
    return statusForTokens(tokens);
  }

  private Response statusForTokens(List<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return Response.ok().entity("token list is empty").build();
    }
    List<ReportFactory.ReportStatus> statuses = reportFactory.Status(tokens);
    return Response.ok().entity(statuses).build();
  }

  /**
   * Get the content of a report.
   * @param token The report Token
   * @return
   */
  @Path("/getreportcontent/{token}")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getReportContent(@PathParam("token") String token) {
    Report report = reportFactory.GetReport(token);
    InputStream content = reportFactory.GetContent(report);
    Response response = Response.ok(content, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"" + report.getFilename() + "\"")
        .build();
    return response;
  }

  /**
   * Get a list of all of the reports that have been craeted and are still stored in the system.
   * @return
   */
  @Path("/getreports")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Report> getAllReports() {
    return reportFactory.ListReports();
  }

  /**
   * Get a list of all the reports a specific user has created and are still stored in the system.
   * @param uuid
   * @return
   */
  @Path("/getuserreports/{uuid}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Report> getUserReports(@PathParam("uuid") UUID uuid) {
    return reportFactory.ListReports(uuid);
  }

  /**
   * Remove a report identified by its unique token from the system.
   * @param token
   */
  @Path("/removereport/{token}")
  @DELETE
  public Response removeReport(@PathParam("token") String token) {
    return reportFactory.RemoveReport(token) ? Response.ok().build(): Responses.notFound().build();
  }

}

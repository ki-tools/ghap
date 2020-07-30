package io.ghap.visualization.publish;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import io.ghap.logevents.Types;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.visualization.publish.data.AppPublishResult;
import io.ghap.visualization.publish.data.PublishDataFactory;
import io.ghap.visualization.publish.search.ShinyAppItem;
import io.ghap.visualization.publish.search.ElasticSearchClient;
import io.searchbox.core.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.amazonaws.util.IOUtils.closeQuietly;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Singleton
@Path("VisualizationPublisher")
public class VisualizationPublicationResource
{
  private final Logger log = LoggerFactory.getLogger(VisualizationPublicationResource.class);

  @Configuration("visualizationPublication.name")
  private String visualizationPublicationName;

  @Inject
  private PublishDataFactory dataFactory;

  @Inject
  private ElasticSearchClient elasticSearchClient;

  /**
   * Contribute a file to the Visualization Publication S3 bucket all files
   * are encrypted server side when stored.
   * 
   * @param inputStream The stream to write to the S3 Bucket
   * @param contentDisposition Content Disposition of the inputStream
   * @return <li>
   * <ul>{@link Status#OK} on success
   * <ul>{@link Status#NOT_FOUND} if the bucket doesn't exist</ul>
   * <ul>{@link Status#PRECONDITION_FAILED} meta-data.json not valid or missing.</ul>
   * <ul>{@link Status#BAD_REQUEST} if the uploaded file isn't a tar.gz file</ul>
   * <ul>{@link Status#NO_CONTENT} on error</ul>
   * </li>
   */
  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "BMGF Administrator", "Administrators", "Data Visualization Publisher"}, predicateType = PredicateType.OR)
  @Path("/publish")
  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response publish(@FormDataParam("file") InputStream inputStream,
                          @FormDataParam("file") FormDataContentDisposition contentDisposition,
                          @FormDataParam("meta") String meta,
                          @Context SecurityContext securityContext)
  {
    String keyName = contentDisposition.getFileName();
    String type = contentDisposition.getType();

    Response response = null;
    File file = null;

    try
    {
      dataFactory.prepare(keyName);
      try {
        file = File.createTempFile("visualization-app-", ".zip");
        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } finally {
        closeQuietly(inputStream, null);
      }
      AppPublishResult result = dataFactory.publish(file, type, keyName, meta);
      response = Response.status(result.getStatus()).entity(result).build();
    }
    catch(IllegalStateException ise)
    {
      log.error(ise.getMessage(), ise);
      return Response.status(Status.NOT_FOUND).entity(ise.getMessage()).build();
    }
    catch(IOException ioe)
    {
      log.error(ioe.getMessage(), ioe);
      return Response.serverError().entity(ioe.getMessage()).build();
    }
    finally {
      if(file != null && file.exists()) {
        file.delete();
      }
    }
    return response;
  }

  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "BMGF Administrator", "Administrators", "Data Viewer", "Data Visualization Publisher"}, predicateType = PredicateType.OR)
  @Path("/search")
  @GET
  public Response search(@QueryParam("query") String query) {
    try {
      List<SearchResult.Hit<Map, Void>> searchResult = elasticSearchClient.read(Types.SHINY_APPS_SEARCH, query);
      List<ShinyAppItem> items = (List<ShinyAppItem>)searchResult.stream().map((it) -> ShinyAppItem.build(it.source)).collect(Collectors.toList());
      return Response.ok().entity(items).build();
    } catch (IOException e) {
      log.error("Search action error", e);
      throw new WebApplicationException(Response.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
    }
  }

  @OAuth20
  @AllowedScopes(scopes = {"GHAP Administrator", "BMGF Administrator", "Administrators", "Data Viewer", "Data Visualization Publisher"}, predicateType = PredicateType.OR)
  @Path("/registry")
  @GET
  public Response registry(@QueryParam("url") String url) {
    return Response.ok().entity(dataFactory.registry(url)).build();
  }

  @Path("/image")
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public InputStream image(@QueryParam("url") String url) {
    return dataFactory.image(url);
  }

  public String getServiceName()
  {
    return visualizationPublicationName;
  }
}

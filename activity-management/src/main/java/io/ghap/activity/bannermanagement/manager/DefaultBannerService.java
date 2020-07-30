package io.ghap.activity.bannermanagement.manager;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import io.ghap.activity.bannermanagement.dao.CommonPersistDao;
import io.ghap.activity.bannermanagement.domain.Banner;
import io.ghap.activity.exception.ApplicationException;
import io.ghap.activity.bannermanagement.model.ApiBanner;
import io.ghap.activity.bannermanagement.model.ApiCreateBanner;
import io.ghap.activity.bannermanagement.model.ApiUpdateBanner;
import io.ghap.activity.bannermanagement.model.DateApiBanner;
import io.ghap.activity.utils.BeanUtils;
import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.RollbackException;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import io.ghap.activity.bannermanagement.model.Error;

@Path("banner")
@Singleton
@AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Curator"}, predicateType = PredicateType.OR)
public class DefaultBannerService implements BannerService {

    private static int MAX_BANNERS_TO_CURRENT_DATE = 2;
    private static final List<String> EXCLUDES = Collections.singletonList("id");
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String CURRENT_BANNERS_QUERY = "select e from Banner e where :date between e.startDate and e.endDate order by e.startDate";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private CommonPersistDao commonPersistDao;

    @Override
    @POST
    @OAuth20
    @Consumes("application/json")
    @Produces("application/json")
    public ApiBanner create(@Context SecurityContext securityContext, @Valid ApiCreateBanner apiBanner) throws ApplicationException {
        log.info("start create banner {}", apiBanner.getId());
        Banner dbBanner = new Banner();
        try {

            //save banner in the DB
            dbBanner.setTitle(apiBanner.getTitle());
            dbBanner.setMessage(apiBanner.getMessage());
            dbBanner.setStartDate(concatDates(apiBanner.getStartDate(), apiBanner.getStartTime()));
            dbBanner.setEndDate(concatDates(apiBanner.getEndDate(), apiBanner.getEndTime()));
            dbBanner.setColor(apiBanner.getColor());
            commonPersistDao.create(dbBanner);

            ApiCreateBanner target = new ApiCreateBanner();
            BeanUtils.copyProperties(dbBanner, target);
            convertDates(dbBanner, target);
            return  target;
        } catch (Throwable e) {
            log.error("error create banner ", e);
            try {
                if (dbBanner.getId() != null) {
                    commonPersistDao.delete(dbBanner);
                }
            } catch (Throwable e1) {
                log.error("error delete banner", e1);
            }
            throw new ApplicationException(e, Collections.singleton(new Error(500, "Unexpected error")));
        }
    }

    @Override
    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public List<? extends DateApiBanner> getAll() throws ApplicationException {

        List<Banner> banners = commonPersistDao.executeQuery(Banner.class, "select e from Banner e order by e.startDate", null);
        List<DateApiBanner> result = new ArrayList<>(banners.size());
        try {
            for (Banner banner : banners) {
                log.error("!!!! banner = " + banner.getId() + " " + banner.getTitle() + " " + banner.getMessage());
                ApiCreateBanner apiBanner = new ApiCreateBanner();
                BeanUtils.copyProperties(banner, apiBanner);
                convertDates(banner, apiBanner);
                log.error("!!!! apibanner = " + apiBanner.getId() + " " + apiBanner.getTitle() + " " + apiBanner.getMessage());
                result.add(apiBanner);
            }
        } catch (ParseException e) {
            throw new ApplicationException(e, Collections.singleton(new Error(500, "Unexpected error")));
        }
        log.error("actual size = " + result.size());
        return result;
    }

    @Override
    @GET
    @Path("/{id}")
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator"}, predicateType = PredicateType.OR)
    public DateApiBanner get(@PathParam("id") UUID id) throws ApplicationException {
        Banner banner = commonPersistDao.read(Banner.class, id);
        validateExists(banner, null);
        ApiCreateBanner apiBanner = new ApiCreateBanner();
        BeanUtils.copyProperties(banner, apiBanner);
        try {
            convertDates(banner, apiBanner);
        } catch (ParseException e) {
            throw new ApplicationException(e, Collections.singleton(new Error(500, "Unexpected error")));
        }
        return apiBanner;
    }

    @Override
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @OAuth20
    @Path("/{id}")
    public DateApiBanner update(@Valid ApiUpdateBanner apiBanner) throws ApplicationException {
        log.info("start update banner {}", apiBanner.getId());
        Banner banner = commonPersistDao.read(Banner.class, apiBanner.getId());
        validateExists(banner, null);

        //check if client send empty values
        if (StringUtils.isEmpty(apiBanner.getMessage()) && StringUtils.isBlank(apiBanner.getTitle())) {
            apiBanner.setId(banner.getId());
            return apiBanner;
        }
        BeanUtils.copyProperties(apiBanner, banner);
        banner.setStartDate(concatDates(apiBanner.getStartDate(), apiBanner.getStartTime()));
        banner.setEndDate(concatDates(apiBanner.getEndDate(), apiBanner.getEndTime()));
        commonPersistDao.update(banner);
        try {
            ApiUpdateBanner target = new ApiUpdateBanner();
            BeanUtils.copyProperties(banner, target);
            convertDates(banner, target);
            return  target;
        } catch (Exception e) {
            log.error("error create banner in stash", e);
            throw new ApplicationException(new HashSet<>(Arrays.asList(new Error(500, e.toString()))));
        }
    }

    @Override
    @DELETE
    @Path("/{id}")
    @OAuth20
    @Produces("application/json")
    public void delete(@PathParam("id") UUID id) throws ApplicationException {
        log.info("start delete banner {}", id);
        Banner banner = commonPersistDao.read(Banner.class, id);
        validateExists(banner, null);
        try {
            log.info("start delete banner {}, {}", id, banner.getId());
            commonPersistDao.delete(banner);
        } catch (RollbackException e1) {
            log.error("error delete entity {} with id {}, messages = {}", banner, id, e1);
        }
    }

    @Override
    @GET
    @Path("/current")
    @Consumes("application/json")
    @Produces("application/json")
    @AllowedScopes(scopes = {"GhapAdministrators", "Administrators", "GHAP Administrator", "Data Analyst", "Data Curator", "Data Viewer", "BMGF Administrator", "Data Contributor", "Data Visualization Publisher"}, predicateType = PredicateType.OR)
    public List<? extends DateApiBanner> current() throws ApplicationException {

        List<Banner> banners = commonPersistDao.executeQuery(Banner.class, CURRENT_BANNERS_QUERY, Collections.singletonMap("date", (Object)new Date()));
        List<DateApiBanner> result = new ArrayList<>();
        try {
            for (int i = 0; i < Math.min(MAX_BANNERS_TO_CURRENT_DATE, banners.size()); i++) {
                Banner banner = banners.get(i);
                ApiCreateBanner apiBanner = new ApiCreateBanner();
                BeanUtils.copyProperties(banner, apiBanner);
                convertDates(banner, apiBanner);
                result.add(apiBanner);
            }
        } catch (ParseException e) {
            throw new ApplicationException(e, Collections.singleton(new Error(500, "Unexpected error")));
        }

        return result;
    }

    private void validateExists(Object entity, String message) {
        if (entity == null) {
            throw (message == null) ? new NotFoundException():new NotFoundException(message);
        }
    }

    private static Date concatDates(Date date, Date time) {
        if (date == null) {
            return null;
        }
        if (time == null) {
            return date;
        }
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(time);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        return c.getTime();
    }

    private static void convertDates(Banner banner, DateApiBanner apiBanner) throws ParseException {
        Date[] dates = splitDateIntoDateTime(banner.getStartDate());
        if (dates != null) {
            apiBanner.setStartDate(dates[0]);
            apiBanner.setStartTime(dates[1]);
        }
        dates = splitDateIntoDateTime(banner.getEndDate());
        if (dates != null) {
            apiBanner.setEndDate(dates[0]);
            apiBanner.setEndTime(dates[1]);
        }
    }

    private static Date[] splitDateIntoDateTime(Date source) throws ParseException {
        if (source == null) {
            return null;
        }
        Date[] result = new Date[2];
        DateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
        String dateStr = df.format(source);
        String[] split = dateStr.split(" ");
        result[0] = new SimpleDateFormat(DATE_FORMAT).parse(split[0]);
        result[1] = new SimpleDateFormat(TIME_FORMAT).parse(split[1]);
        return result;
    }

}

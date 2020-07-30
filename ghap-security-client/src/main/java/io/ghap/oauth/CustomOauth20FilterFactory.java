package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.github.hburgmeier.jerseyoauth2.rs.impl.filter.OAuth20FilterFactory;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.api.model.AbstractResourceMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
public class CustomOauth20FilterFactory extends OAuth20FilterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth20FilterFactory.class);

    @Override
    public List<ResourceFilter> create(AbstractMethod am) {
        if (am instanceof AbstractResourceMethod)
        {
            OAuth20 oauth20 = am.getAnnotation(OAuth20.class);
            if(oauth20 == null){
                oauth20 = am.getResource().getAnnotation(OAuth20.class);
            }

            if ( oauth20 != null)
            {
                AllowedScopes scopes = am.getAnnotation(AllowedScopes.class);
                if(scopes != null){
                    LOGGER.debug("Installing oauth2 filter on {} for scopes: {}", am, asList(scopes));
                } else {
                    scopes = am.getResource().getAnnotation(AllowedScopes.class);
                    if(scopes != null && scopes.scopes() != null && scopes.scopes().length > 0){
                        LOGGER.debug("Installing oauth2 filter on {} for class scopes: {}", am, asList(scopes));
                    } else {
                        LOGGER.debug("Allowed scopes for {} are not defined", am);
                    }
                }
                return __getFilters(scopes);

            }
            return null;
        } else
            return null;
    }

    protected List<ResourceFilter> __getFilters(AllowedScopes scopes) {
        List<ResourceFilter> securityFilters = new LinkedList<ResourceFilter>();
        OAuth20AuthenticationFilter oAuth20AuthenticationFilter = new OAuth20AuthenticationFilter(getAccessTokenVerifier(),
                getRSConfiguration(), getRequestFactory());
        if (scopes!=null && scopes.scopes().length>0)
        {
            LOGGER.debug("Installing scope filter");
            oAuth20AuthenticationFilter.setRequiredScopes(scopes.scopes());
            oAuth20AuthenticationFilter.setPredicateType(scopes.predicateType());
        }
        securityFilters.add(oAuth20AuthenticationFilter);
        return securityFilters;
    }
}

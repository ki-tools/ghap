package io.ghap.user.manager.impl;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.List;

public class PaginateAnything {

    public static <T> List<T> paginate(List<T> items, HttpServletResponse servletResponse, String rangeStr){
        String[] range = (rangeStr == null) ? null:rangeStr.split("-");
        if(range != null){
            if(range.length != 2){
                throw new WebApplicationException( Response.status(400).entity("Malformed \"Range\" header format").build() );
            }

            try {
                final int total = items.size();
                final int min = Integer.parseInt(range[0].trim());
                final int max = Integer.parseInt(range[1].trim());

                int i = 0;
                for (final Iterator it = items.iterator(); it.hasNext(); ) {
                    it.next();
                    if (i < min || i > max) {
                        it.remove();
                    }
                    i++;
                }
                // Prepare "Content-Range" header. See: https://github.com/begriffs/angular-paginate-anything
                final int lastIndex = total - 1;
                final int from = (lastIndex < min) ? 0 : min;
                final int to = (max <= lastIndex) ? (min >= lastIndex ? 0 : max) : (lastIndex < min ? 0 :lastIndex);

                servletResponse.addHeader("Accept-Ranges", "items");
                servletResponse.addHeader("Content-Range", "items " + from + "-" + to + "/" + total);
                servletResponse.addHeader("Range-Unit", "items");
            } catch (NumberFormatException e){
                throw new WebApplicationException( Response.status(400).entity("Malformed \"Range\" header format. NumberFormatException.").build() );
            }
        }
        return items;
    }
}

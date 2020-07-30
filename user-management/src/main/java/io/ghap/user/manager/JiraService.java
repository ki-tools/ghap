package io.ghap.user.manager;

import com.sun.jersey.multipart.FormDataMultiPart;

public interface JiraService {
    String bugReport(FormDataMultiPart formParams);
}

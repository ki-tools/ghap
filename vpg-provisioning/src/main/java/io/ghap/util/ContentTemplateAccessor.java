package io.ghap.util;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STRawGroupDir;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

@Singleton
public class ContentTemplateAccessor {

  private STGroup stGroup;

  @PostConstruct
  public void loadContentTemplates() {
    this.stGroup = new STRawGroupDir("templates", '$', '$');
  }


  public String getTemplate(String templateName, Map<String, String> parameters) {

    ST template = loadTemplate(templateName);

    for (Map.Entry<String, String> param : parameters.entrySet()) {
      template.add(param.getKey(), param.getValue());
    }

    return template.render().replaceAll(">\\s+<", "><");

  }

  private ST loadTemplate(String name) {
    //see: http://stackoverflow.com/questions/14595500/how-to-escape-a-stringtemplate-template
    return stGroup.getInstanceOf(name);
  }

}

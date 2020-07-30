'use strict';

/* globals ActiveXObject */

/**
 * @ngdoc service
 * @name bmgfApp.Settings
 * @description
 * # Settings
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Settings', function () {

    var obj = {
      // // DEV
      // API_ROOT:              'http://userservice.dev.ghap.io',
      // OAUTH_URL:             'http://oauth.dev.ghap.io',
      // PROVISIONING_ROOT:     'http://provisioningservice.dev.ghap.io/rest/v1',
      // ACTIVITY_ROOT:         'http://activityservice.dev.ghap.io/rest/v1',
      // PROJECT_ROOT:          'http://projectservice.dev.ghap.io/rest/v1',
      // BANNER_ROOT:           'http://banner.dev.ghap.io/rest/v1',
      // USERDATA_ROOT:         'http://userdata.dev.ghap.io/rest/v1',
      // DATA_SUBMISSIONS_ROOT: 'http://datasubmissions.dev.ghap.io/rest/v1',
      // JIRA_API_URL:          'http://www.tripos.com/web-services/submit-error-bmgf.php',
      // SHINY_SERVER_URL:      'https://shiny-server-pro.dev.ghap.io/test-apps',
      // VISUALIZATION_URL:     'http://visualization-publisher.dev.ghap.io/rest/v1',
      // REPORT_API_URL:        'http://reportingservice.dev.ghap.io/rest/v1/Reporting',
      // QA
      //API_ROOT:              'http://userservice.qa.ghap.io',
      //OAUTH_URL:             'http://oauth.qa.ghap.io',
      //PROVISIONING_ROOT:     'http://provisioningservice.qa.ghap.io/rest/v1',
      //ACTIVITY_ROOT:         'http://activityservice.qa.ghap.io/rest/v1',
      //PROJECT_ROOT:          'http://projectservice.qa.ghap.io/rest/v1',
      //BANNER_ROOT:           'http://banner.qa.ghap.io/rest/v1',
      //USERDATA_ROOT:         'http://userdata.qa.ghap.io/rest/v1',
      //DATA_SUBMISSIONS_ROOT: 'http://datasubmissions.qa.ghap.io/rest/v1',
      //JIRA_API_URL:          'http://www.tripos.com/web-services/submit-error-bmgf.php',
      // SHINY_SERVER_URL:      'https://shiny-server-pro.qa.ghap.io/test-apps',
      // SHINY_SERVER_URL:      'https://localhost',
      // VISUALIZATION_URL:     'http://visualization-publisher.qa.ghap.io/rest/v1',
      // REPORT_API_URL:        'http://reportingservice.qa.ghap.io/rest/v1/Reporting',
      // SAMBA
      API_ROOT:              'http://userservice.samba.ghap.io',
      OAUTH_URL:             'http://oauth.samba.ghap.io',
      PROVISIONING_ROOT:     'http://provisioningservice.samba.ghap.io/rest/v1',
      ACTIVITY_ROOT:         'http://activityservice.samba.ghap.io/rest/v1',
      PROJECT_ROOT:          'http://projectservice.samba.ghap.io/rest/v1',
      USERDATA_ROOT:         'http://userdata.samba.ghap.io/rest/v1',
      DATA_SUBMISSIONS_ROOT: 'https://datasubmissions.samba.ghap.io/rest/v1',
      BANNER_ROOT:           'https://activityservice.samba.ghap.io/rest/v1/banner',
      JIRA_API_URL:          'http://localhost:8080/issues/submit-error',
      SHINY_SERVER_URL:      'http://visualizations-proxy-service.samba.ghap.io',
      //SHINY_SERVER_URL:      'https://visualizations.samba.ghap.io/',
      //SHINY_SERVER_URL:      'https://localhost',
      VISUALIZATION_URL:     'http://visualization-publisher.samba.ghap.io/rest/v1',
      REPORT_API_URL:        'http://reportingservice.samba.ghap.io/rest/v1/Reporting',
      KNOWLEDGE_BASE_URL:    'https://www.google.com'

    };

    var xhr;
    try {
      xhr = new ActiveXObject('Msxml2.XMLHTTP');
    } catch (e) {
      try {
        xhr = new ActiveXObject('Microsoft.XMLHTTP');
      } catch (E) {
        xhr = false;
      }
    }

    if (!xhr && typeof XMLHttpRequest !== 'undefined') {
      xhr = new XMLHttpRequest();
    }

    xhr.open('GET', '/settings', false);
    xhr.send(null);
    if(xhr.status === 200) {
      var tmpObj = JSON.parse(xhr.responseText);
      if (tmpObj.OAUTH_URL) {
        obj = tmpObj;
      }
    }

    return obj;
  });

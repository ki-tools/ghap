/**
 * Created by Vlad on 29.08.2016.
 */

var cfg = require('./../Common/ghap-config');

var
    service_path = cfg.vzPublisherService,
    resources_path = '/rest/v1/VisualizationPublisher',
    proxy_path = cfg.vzProxyService;

exports.getRegistry_url = function(access_token) {
    return service_path + resources_path + '/registry?url=' +
        encodeURIComponent(proxy_path + '/registry.json?token=' + access_token);
};

module.exports = {};
exports = module.exports;

var cfg = require('./../Common/ghap-config');

var
	service_path = cfg.activityservice,
	banner_resource_path = '/rest/v1';

exports.getCurrentBanner_Url = function () {
    return service_path + banner_resource_path + '/banner/current'
};

exports.getBanner_Url = function () {
    return service_path + banner_resource_path + '/banner'
};

exports.getBannerById_Url = function (id) {
    return service_path + banner_resource_path + '/banner/' + id;
};

/**
 * Created by Vlad on 15.01.2016.
 */

var cfg = require('./../Common/ghap-config');

var
    service_path = cfg.reportingservice,
    requests_path = '/rest/v1/Reporting';

exports.getAvailableReports_url = function() {
    return service_path + requests_path + '/getavailablereports';
};

exports.createReport_url = function(user_id, report_type) {
    return service_path + requests_path + '/create/' + user_id + '/' + report_type;
};

exports.createConstrainedReport_url = function(user_id, report_type) {
    return service_path + requests_path + '/constrainedcreate/' + user_id + '/' + report_type;
};

exports.getUserReports_url = function(user_id) {
    return service_path + requests_path + '/getuserreports/' + user_id;
};

exports.getStatuses_url = function() {
    return service_path + requests_path + '/getstatuses';
};

exports.getStatus_url = function(token) {
    return service_path + requests_path + '/getstatus/' + token;
};

exports.deleteReport_url = function(token) {
    return service_path + requests_path + '/removereport/' + token;
};

exports.getReportContent_url = function(access_token, report_token) {
    return service_path + requests_path + '/getreportcontent/' + report_token + '?token=' + access_token;
};

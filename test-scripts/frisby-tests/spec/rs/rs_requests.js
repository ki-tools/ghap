/**
 * Created by Vlad on 15.01.2016.
 */

var my = require('./../Common/ghap-lib');
var cfg = require('./../Common/ghap-config');
var ghapFrisby = require('./../Common/ghap-frisby');

var rsUrls = require('./rs_urls');

/**
 * @typedef {object} GhapReport
 * @property {string] type - type id ('USER_STATUS','ROLE_STATUS', etc.).
 * @property {string] categoryName - category ('Auditing','Usage').
 * @property {string] typeName - type name (i.e.'User Accounts').
 * @property {string[]} constraintTypes - array of constraints (i.e.['DATA_RANGE']).
 */

/**
 * Get a list of the reports that the system is capable of generating.
 * @param {GhapAuthHeader} authHeader
 * @returns {Promise.<[GhapReport]>}
 */
module.exports.getAvailableReports = function(authHeader){
    return ghapFrisby.create(authHeader, 'Get Available Reports')
        .get(rsUrls.getAvailableReports_url())
        .onSuccess(function (body) {
            return my.jsonParse(body);
        })
        .corsOn(cfg.origin)
        .returnPromise();
};

/**
 * Create a new Report
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUser} ums_user
 * @param {GhapReport} ghap_report
 * @returns {Promise.<string>} promise resolved with report's token or rejected if error
 */
module.exports.createReport = function(authHeader, ums_user, ghap_report){
    return ghapFrisby.create(authHeader, 'Create Report')
        .put(rsUrls.createReport_url(ums_user.getGuid(), ghap_report.type))
        .setLogMessage("Create '%s' report for user '%s'", ghap_report.typeName, ums_user.getName())
        .onSuccess(function (body) {
            expect(typeof body).toBeType('string');
            console.log(" Token received: '%s'", body);
            return body;
        })
        .corsOn(cfg.origin)
        .returnPromise();
};
/**
 * Create a new Report
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUser} ums_user
 * @param {GhapReport} ghap_report
 * @param {Array} constrains
 * @returns {Promise.<string>} promise resolved with report's token or rejected if error
 */
module.exports.createConstrainedReport = function(authHeader, ums_user, ghap_report, constrains){
    return ghapFrisby.create(authHeader, 'Create Constrained Report')
        .put(rsUrls.createConstrainedReport_url(ums_user.getGuid(), ghap_report.type),constrains, {json: true})
        .setLogMessage("Create '%s' constrained report for user '%s'", ghap_report.typeName, ums_user.getName())
        .onSuccess(function (body) {
            expect(typeof body).toBeType('string');
            console.log(" Token received: '%s'", body);
            return body;
        })
        .corsOn(cfg.origin)
        .returnPromise();
};

/**
 * @typedef {Object} UserReport
 * @property {string] token
 * @property {string} owner - User GUID
 * @property {timestamp} created
 * @property {string} name - report name
 * @property {string} reportType
 * @property {string} contentType
 * @property {string} filename
 */

/**
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUser} ums_user
 * @returns {Promise.<[UserReport]>}
 */
module.exports.getUserReports = function(authHeader, ums_user){
    return ghapFrisby.create(authHeader, 'Get User Reports')
        .get(rsUrls.getUserReports_url(ums_user.getGuid()))
        .setLogMessage("Get reports list for user '%s'", ums_user.getName())
        .onSuccess(function (body) {
            return my.jsonParse(body);
        })
        .corsOn(cfg.origin)
        .returnPromise();
};

/**
 * Get the statuses of a group of reports identified by an list of tokens.
 * @param {GhapAuthHeader} authHeader
 * @param {string[]} tokens - array of report tokens
 * @returns {Promise.<string>} - comma separated list of report statuses
 */
module.exports.getStatuses = function(authHeader, tokens){
    return ghapFrisby.create(authHeader, 'Get Reports Statuses')
        .post(rsUrls.getStatuses_url(), tokens, {json:true})
        .addHeader('content-type', 'application/json')
        .onSuccess(function (body) {
            return my.jsonParse(body);
        })
        .corsOn(cfg.origin)
        .returnPromise();
};

/**
 * Get the status of a report identified by a token.
 * If a report is still in the process of being created a status of ACCEPTED will be returned,
 * if a user requests a token that is not available a NOT_FOUND status will be returned,
 * if an error is encountered a SERVER_ERROR will be returned.
 * Otherwise the reponse will be OK, and contain a Report entity.
 *
 * @param {GhapAuthHeader} authHeader
 * @param {string} token - report token
 * @returns {Promise} resolved with response or rejected if error happens
 */
module.exports.getReportStatus = function(authHeader, token){
    return ghapFrisby.create(authHeader, 'Get Report Status')
        .get(rsUrls.getStatus_url(token))
        .addHeader('content-type', 'application/json')
        .onSuccess(function (body) {
            return my.jsonParse(body);
        })
        .corsOn(cfg.origin)
        .returnPromise();
};

/**
 * Remove a report identified by its unique token from the system.
 * @param {GhapAuthHeader} authHeader
 * @param {string} token - report token
 * @returns {Promise} - resolved with empty value or rejected if error
 */
module.exports.deleteReport = function(authHeader, token){
    return ghapFrisby.create(authHeader, 'Delete Report')
        .delete(rsUrls.deleteReport_url(token))
        .corsOn(cfg.origin)
        .returnPromise();
};
/**
 *
 * @param {OAuth} oAuth
 * @param {string} report_token
 * @returns {Promise.<string>} - resolved with text of report or rejected if error
 */
module.exports.getReportContent = function(oAuth, report_token){
    return ghapFrisby.create(oAuth.header, 'Get Report Content')
        .get(rsUrls.getReportContent_url(oAuth.access_token, report_token))
        .onSuccess(function (body) {
            expect(typeof body).toBe('string');
            return body;
        })
        .returnPromise();
};

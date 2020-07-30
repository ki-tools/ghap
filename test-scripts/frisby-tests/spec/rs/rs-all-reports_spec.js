/**
 * Created by Vlad on 20.01.2016.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'RS-ALL';
my.logModuleName(module.filename);

var rsRequests = require('./rs_requests');
var createdReportTokens = [];

var umsRequests = require('../ums/ums_requests');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(function() {
        return umsRequests.pullUser(oAuth, testerAdmin)
            .then(function () {
                console.log(" user GUID '%s'", testerAdmin.getGuid())
            })
    })
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

function runSuite() {
    return rsRequests.getAvailableReports(oAuth.header)
        .then(processAllReports)
        .then(deleteCreatedReports)
}

function processAllReports(available_reports) {
    console.log(" %d reports are available.",available_reports.length);
    expect(available_reports.length).toBeGreaterThan(6);

    // Q: q promise one by another
    // A: http://stackoverflow.com/questions/24586110/resolve-promises-one-after-another-i-e-in-sequence
    var p = Q();
    available_reports.forEach(function(report) {
        if (report.categoryName === 'Auditing') {
            p = p.then(processAuditingReport.bind(this,report))
        } else if (report.categoryName === 'Usage') {
            p = p.then(processUsageReport.bind(this,report))
        } else {
            throw new Error("Unknown report category'"+report.categoryName+"'");
        }
    });
    return p;
}

/**
 * Create and validate report of Auditing type
 * @param {GhapReport} report
 * @returns {Q.Promise<U>}
 */
function processAuditingReport(report) {
    return rsRequests.createReport(oAuth.header, testerAdmin, report)
        .then(validateReport)
}

/**
 * Create and validate report of Usage type
 * @param {GhapReport} report
 * @returns {Promise}
 */
function processUsageReport(report) {
    var constrains = [{type: "DATE_RANGE", constraint: {start: "2016-01-01", end: "2016-01-01"}}];
    return rsRequests.createConstrainedReport(oAuth.header, testerAdmin, report, constrains)
        .then(validateReport)
}

function validateReport(report_token) {
    createdReportTokens.push(report_token);
    return rsRequests.getReportStatus(oAuth.header, report_token)
        .then(function (report_status) {
            console.log(" Report status: '%s'", report_status);
            expect(report_status).toBe('COMPLETE');
            return rsRequests.getReportContent(oAuth, report_token)
        })
        .then(function (content) {
            console.log(" %d characters received.", content.length);
            expect(content.length).toBeGreaterThan(200);
        })
}

function deleteCreatedReports() {
    var p = Q();
    createdReportTokens.forEach(function(report_token) {
        p = p.then(rsRequests.deleteReport.bind(this,oAuth.header,report_token))
    });
    return p;
}
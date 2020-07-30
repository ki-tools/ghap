var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'BannerSrv';
my.logModuleName(module.filename);

var bannersResources = require('./as_banners_resources');
var bannersRequests = require('./as_banners_requests');

var banner = bannersResources.makeBanner();
banner.title = "Test Banner";
banner.message = "Created at " + new Date().toDateString();

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();

bannersRequests.getCurrentBanner()
    .then(function () {
        return oAuth.login(testerAdmin.getName(), testerAdmin.getPassword());
    })
    .then(function(oA){
        testerAdmin.setAuthHeader(oA.header);
    })
    .then(runAuthorizedRequests)
    .catch(my.reportError)
    .finally(finalCase);

function runAuthorizedRequests() {
    return bannersRequests.createBanner(testerAdmin.authHeader, banner)
        .then(validateBannersList)
        .then(function () {
            return bannersRequests.getBanner(testerAdmin.authHeader, banner.id)
        })
        .then(validateInvalidCreateParams)
        .then(updateBanner)
        .then(function () {
            return bannersRequests.deleteBanner(testerAdmin.authHeader, banner);
        })
}

function validateBannersList() {
    return bannersRequests.getBanners(testerAdmin.authHeader)
        .then(function (bannersList) {
            var deferred = Q.defer();
            my.describe("Validate banners list", function () {
                my.it(" - last created banner should be present in the list", function () {
                    var bn = my.findElementInArray(bannersList, "id", banner.id);
                    expect(bn).not.toBe(null);
                    deferred.resolve();
                    waits(100); // wait while promise will be fulfilled
                })
            });
            return deferred.promise;
        })
}

function validateInvalidCreateParams() {
    var invalidBanner = bannersResources.makeBanner();
    var isCreateRequestRejected = false;
    return bannersRequests.createBanner(testerAdmin.authHeader, invalidBanner,
        function (responseStatus, body) {
            if (responseStatus == 400) {
                isCreateRequestRejected = true;
                var response = my.jsonParse(body);
                expect(response.success).toBeFalsy();
                expect(response.errors).toBeType(Array);
                if (response.errors instanceof Array) {
                    expect(response.errors.length).toBeGreaterThan(1);
                    var isMsgLengthErr = false;
                    var isTitleLengthErr = false;
                    for(var i = 0; i < response.errors.length;i++) {
                        var errMsg = response.errors[i].msg.toLowerCase();
                        if (errMsg.indexOf("message length")) isMsgLengthErr = true;
                        if (errMsg.indexOf("title length")) isTitleLengthErr = true;
                    }
                    expect(isMsgLengthErr).toBeTruthy();
                    expect(isTitleLengthErr).toBeTruthy();
                    if (isMsgLengthErr && isTitleLengthErr)
                        return true;
                }
            }
            return false;
        })
        .then(function () {
            expect(isCreateRequestRejected).toBeTruthy();
            if (isCreateRequestRejected)
                console.log("Create request was rejected with expected errors messages");
            else
                console.error("Create request was successful but expected to be rejected.")
        })
}

function updateBanner() {
    var updatedBanner = bannersResources.makeBanner();
    updatedBanner.id = banner.id;
    updatedBanner.title = "Test Banner updated";
    updatedBanner.message = "Updated at " + new Date().toISOString();
    updatedBanner.startDate = "1970-01-01";
    updatedBanner.startTime = "23:00";
    updatedBanner.endDate = "2100-01-01";
    updatedBanner.endTime = "01:00";
    updatedBanner.color = bannersResources.redColor;

    return bannersRequests.updateBanner(testerAdmin.authHeader, updatedBanner)
        .then(function () {
            my.copyProperties(updatedBanner, banner);
        })
}

function finalCase() {
    my.finalTest();
}
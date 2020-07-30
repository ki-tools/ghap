/**
 * Created by Vlad on 29.08.2016.
 */
var assert = require('chai').assert;

var oAuthService = require("./../../frisby-tests/spec/utils/ghap-oauth-promise");
var oAuth = oAuthService.makeOAuthClient();
var ghapRq = require('./../../frisby-tests/spec/utils/ghap-requests');
ghapRq.sendCorsRequests = true;

var tester = require("./test_user").getTester();

describe("Login",function () {

    it("Authorize",function () {
        var promise = oAuth.login(tester.getName(), tester.getPassword());
        return assert.isFulfilled(promise);
    });

    it("Load user data", function () {
        return assert.isFulfilled(
            ghapRq.getCurrentUser(oAuth.header)
                .then(function (ums_user) {
                    tester = ums_user;
                    tester.setAuthHeader(oAuth.header);
                    require("./test_user").setTester(tester);
                })
        )
    })
});

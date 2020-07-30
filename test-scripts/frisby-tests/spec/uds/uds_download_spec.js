/**
 * Created by Vlad on 22.12.2015.
 */

var fs = require('fs');
var mod_path = require('path');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'UserDataUpload';
my.logModuleName(module.filename);

var umsRequests = require('../ums/ums_requests');
var umsUser = require('../ums/ums_user');
var Tester = umsUser.makeUser('vlad.ruzov.uds.tester');
Tester.setPassword("");

var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(Tester.getName(), Tester.getPassword())
    .then(umsRequests.validateToken)
    .then(function(){return umsRequests.pullUser(oAuth, Tester)})
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

var udsRequests = require('./uds_requests');

function runSuite() {
    return udsRequests.downloadFile(oAuth, Tester, '', 'logo-frisby.png')
        .then(function(data) {
            var logoPath = mod_path.resolve(__dirname, 'logo-frisby.png');
            var logo_file_size = fs.statSync(logoPath).size;
            console.log(" %d bytes downloaded.", logo_file_size);
            expect(data.length).toBe(logo_file_size);
        })
}

var Q = require('q');

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

var udsUrls = require('./uds_urls');
var udsRequests = require('./uds_requests');

var frisby = require('frisby');
var fs = require('fs');
var path = require('path');
var FormData = require('form-data');

function runSuite() {
    var logoPath = path.resolve(__dirname, 'logo-frisby.png');
    return udsRequests.uploadFile(oAuth.header, Tester, '', logoPath);
}

function runSuite2() {
    var deferred = Q.defer();

    // based on the following example ( GQ 'frisby upload' --> https://github.com/vlucas/frisby/issues/84 )
    // https://github.com/kreutter/frisby/blob/master/examples/httpbin_multipart_spec.js
    var logoPath = path.resolve(__dirname, 'logo-frisby.png');
    var form = new FormData();

    form.append('file', fs.createReadStream(logoPath), {
        knownLength: fs.statSync(logoPath).size         // we need to set the knownLength so we can call  form.getLengthSync()
    });

    frisby.create('Upload Frisby Logo')
        .put(udsUrls.submitData_url(Tester.getGuid(),''), form)
        .addHeader(oAuth.header.Name, oAuth.header.Value)
        .addHeader('content-type', 'multipart/form-data; boundary=' + form.getBoundary())
        .addHeader('content-length',form.getLengthSync())
        .after(function (err, response, body){
            deferred.resolve();
        })
        .expectStatus(200)
        .toss();

    return deferred.promise;
}

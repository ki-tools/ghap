/**
 * Created by vruzov on 14.09.2015.
 */

var my = require('./../Common/ghap-lib');
var cfg = require('./../Common/ghap-config');
var frisby  = require('frisby');

var Q = require('q');
var util = require('util');

var debugLog = false;

function OAuth(){
    var
        oauth_Domain    = cfg.oauthDomain,
        oauth_LoginPath = '/oauth/authorize',
        client_id       = 'projectservice',
        response_type   = 'token',
        redirect_uri    = 'http://ghap.io',
        action_url      = '/j_spring_security_check',
        revoke_url      = '/oauth/revoke';

    this.getAuthorize_Url = function() {
        return oauth_Domain + oauth_LoginPath
            + '?client_id=' + client_id
            +'&response_type=' + response_type
            +'&redirect_uri=' + redirect_uri;
    };

    this.getAction_Url= function() {
        return oauth_Domain +  action_url;
    };

    this.getRevoke_Url= function() {
        return oauth_Domain +  revoke_url;
    };

    this.access_token = null;
    this.username = '';
    this.password = '';
    this.jsession_cookie = '';

    /**
     * @typedef {object} GhapAuthHeader
     * @property {string} Name - 'Authorization'
     * @property {string} Value - 'Bearer ' + token
     *
     * @type {GhapAuthHeader} */
    this.header = {
        Name: 'Authorization',
        Value: 'Bearer '
    };

    return this;
}

/**
 *
 * @param {string} username
 * @param {string} password
 * @returns {OAuth}
 */
OAuth.prototype.login = function(username, password){

    console.log("Start oAuth authorization for user '%s'.",username);
    var self = this;
    // reset parameters for reuse
    self.access_token = null;
    self.header.Value = 'Bearer ';
    self.username = username;
    self.password = password;

    var deferred = Q.defer();

    var start = new Date();
    my.logStepNumber();
    frisby.create(my.getStepNumStr() + ' OAuth Get JSESSIONID')
        .get(self.getAuthorize_Url(), {followRedirect: false})
        .expectStatus(302)
        .after(function (err, response, body) {
            if (debugLog){
                console.log("GET request '%s'",self.getAuthorize_Url());
                console.log('GET response status code '+response.statusCode);
                console.log('Response headers:');
                console.log(response.headers);
            }
            if (response.statusCode != 302) {
                var err_message = util.format("Unexpected response status code %d on Get JSESSIONID request.", response.statusCode);
                console.error(err_message);
                deferred.reject(new Error(err_message));
                return
            }
            expect(response.headers['set-cookie']).toBeDefined();
            var cookie_str = getJSESSSIONID_cookie_str(response);
            expect(cookie_str).not.toBeNull();
            if (cookie_str === null) {
                err_message = "JSESSIONID not found in GET response.";
                console.error(err_message);
                deferred.reject(new Error(err_message));
                return
            }
            if (debugLog) console.log('Step 1 success ---------------------------------------');
            my.logStepNumber();
            frisby.create(my.getStepNumStr() + ' OAuth Authorize')
                .post(self.getAction_Url(), {
                    j_username: username,
                    j_password: password,
                    'user-policy': 'on'
                })
                .addHeader('cookie', cookie_str)
                .expectStatus(302)
                .after(function (err, response, body) {

                    if (debugLog) {
                        console.log("POST request '%s'", self.getAction_Url());
                        var request = response.request;
                        console.log('Request headers:');
                        console.log(request.headers);
                        console.log('Request body:');
                        console.log(request.body.toString());
                        console.log('Response status code '+response.statusCode);
                        console.log('Response headers:');
                        console.log(response.headers);
                    }

                    if (response.statusCode != 302) {
                        err_message = util.format("Unexpected response status code %d on Authorize request.", response.status_code);
                        console.error(err_message);
                        deferred.reject(new Error(err_message));
                        return
                    }
                    expect(response.headers.location).toBeDefined();
                    if (!response.headers.hasOwnProperty('location')) {
                        err_message = "Location header is not specified in Authorize response.";
                        console.error(err_message);
                        deferred.reject(new Error(err_message));
                        return
                    }
                    var location = response.headers.location;

                    expect(response.headers['set-cookie']).toBeDefined();
                    var cookie_str = getJSESSSIONID_cookie_str(response);
                    expect(cookie_str).not.toBeNull();
                    if (cookie_str === null) {
                        err_message = "Second JSESSIONID not found in Authorize response.";
                        console.error(err_message);
                        if (location.indexOf('login_error') > -1)
                            console.warn("Check that password '%s' for user '%s' is correct.", password, username);
                        deferred.reject(new Error(err_message));
                        return
                    } else
                        self.jsession_cookie = cookie_str;

                    if (debugLog) console.log('Step 2 success ---------------------------------------');
                    my.logStepNumber();
                    frisby.create(my.getStepNumStr() + ' OAuth Get Token')
                        .get(location, {followRedirect: false})
                        .addHeader('cookie', cookie_str)
                        .expectStatus(302)
                        .after(function (err, response, body) {
                            if (response.statusCode != 302) {
                                err_message = util.format("Unexpected response status code %d on Get token request.", response.statusCode);
                                console.error(err_message);
                                deferred.reject(new Error(err_message));
                                return
                            }
                            expect(response.headers.location).toBeDefined();
                            if (!response.headers.hasOwnProperty('location')) {
                                err_message = "Location header is not specified in Get token response.";
                                console.error(err_message);
                                deferred.reject(new Error(err_message));
                                return
                            }
                            location = response.headers.location;

                            var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
                            var matches = location.match(rePattern);
                            expect(matches[0]).toBeDefined();
                            if (matches == null) {
                                err_message = util.format("'access_token' parameter not found in location header '%s'", location);
                                console.error(err_message);
                                deferred.reject(new Error(err_message));
                                return
                            }

                            self.access_token = matches[0].substring("access_token=".length);
                            var exec_time_ms = new Date() - start;
                            console.info("\nAccess token '%s' for user '%s' is received within %dms", self.access_token, username, exec_time_ms);
                            self.header.Value = 'Bearer ' + self.access_token;
                            deferred.resolve(self);
                            waits(100); // wait while promise will be fulfilled

                        })
                        .toss();

                })
                .toss();
        })
        .toss();

    return deferred.promise;

};

OAuth.prototype.revoke = function() {
    my.logStepNumber();
    frisby.create(my.getStepNumStr() + 'OAuth revoke')
        .get(this.getRevoke_Url())
        .addHeader(this.header.Name, this.header.Value)
        .expectStatus(200)
        .after(function (err, response, body) {
            if (err)
                console.log(err);
            else
                console.log("oAuth revoke token response status code '%d'", response.statusCode)
        })
        .toss();
};

module.exports.makeOAuthClient = function(){return new OAuth()};

function getJSESSSIONID_cookie_str(response) {
    var jsessionid_cookie_str = null;
    if (response.headers.hasOwnProperty('set-cookie'))
        response.headers['set-cookie'].forEach(function (cookie_str) {
            if (cookie_str.indexOf('JSESSIONID') > -1)
                jsessionid_cookie_str = cookie_str;
        });
    return jsessionid_cookie_str;
}
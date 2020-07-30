var Qs = require('qs');
var request = require('request');
var Q = require('q');
var util = require('util');

var cfg = require('./../Common/ghap-config');

/**
 * @typedef {Object} GhapOAuth
 * @property {Function} login
 * @property {String} username
 * @property {String} password
 * @property {String} access_token
 * @property {GhapAuthHeader} authHeader
 */
/**
 * Constructor of GhapOAuth
 * @returns {GhapOAuth}
 * @constructor
 */
function GhapOAuth() {
    var
        oauth_Domain    = cfg.oauthDomain,
        oauth_LoginPath = '/oauth/authorize',
        client_id       = 'projectservice',
        response_type   = 'token',
        redirect_uri    = 'http://ghap.io',
        action_url      = '/j_spring_security_check';

    this.getAuthorize_Url = function() {
        return oauth_Domain + oauth_LoginPath
            + '?client_id=' + client_id
            +'&response_type=' + response_type
            +'&redirect_uri=' + redirect_uri;
    };

    this.getAction_Url= function() {
        return oauth_Domain +  action_url;
    };

    this.access_token = null;
    this.username = '';
    this.password = '';

    /**
     * @type {GhapAuthHeader}
     */
    this.header = {
        Name: 'Authorization',
        Value: 'Bearer '
    };

    return this;
}

/**
 * Login using username and password
 * @param username
 * @param password
 * @returns {promise} resolved with self reference or rejected on error
 */
GhapOAuth.prototype.login = function (username, password) {

    var self = this;
    console.log("Start OAuth authorization for user '%s'.", username);
    // reset parameters for reuse
    self.access_token = null;
    self.header.Value = 'Bearer ';
    self.username = username;
    self.password = password;

    var deferred = Q.defer();

    var start = new Date();
    request({
        url: self.getAuthorize_Url(),
        method: 'GET',
        followRedirect: false
    }, function (error, response, body) {
        if (error) {
            console.error('Can not GET %s',self.getAuthorize_Url());
            console.error(error);
            deferred.reject(error);
            return;
        }
        if (response.statusCode != 302) {
            var err_message = util.format("Unexpected response status code %d on Get JSESSIONID request.", response.statusCode);
            console.error(err_message);
            deferred.reject(new Error(err_message));
            return
        }
        var cookie_str = getJSESSSIONID_cookie_str(response);
        if (cookie_str === null) {
            err_message = "JSESSIONID not found in GET response.";
            console.error(err_message);
            deferred.reject(new Error(err_message));
            return
        }

        request({
            url: self.getAction_Url(),
            method: 'POST',
            headers: {
                'content-type': 'application/x-www-form-urlencoded',
                'cookie': cookie_str
            },
            json: false,
            body: Qs.stringify({
                j_username: username,
                j_password: password
            })
        }, function (error, response, body) {
            if (error) {
                console.error('Can not POST %s',self.getAuthorize_Url());
                console.error(error);
                deferred.reject(error);
                return;
            }
            if (response.statusCode != 302) {
                err_message = util.format("Unexpected response status code %d on Authorize request.", response.status_code);
                console.error(err_message);
                deferred.reject(new Error(err_message));
                return
            }

            if (!response.headers.hasOwnProperty('location')) {
                err_message = "Location header is not specified in Authorize response.";
                console.error(err_message);
                deferred.reject(new Error(err_message));
                return
            }
            var location = response.headers.location;

            var cookie_str = getJSESSSIONID_cookie_str(response);
            if (cookie_str === null) {
                err_message = "Second JSESSIONID not found in Authorize response.";
                console.error(err_message);
                if (location.indexOf('login_error') > -1)
                    console.warn("Check that password '%s' for user '%s' is correct.", password, username);
                deferred.reject(new Error(err_message));
                return
            }

            request({
                url: location,
                method: 'GET',
                headers: {
                    'cookie': cookie_str
                },
                followRedirect: false
            }, function (error, response, body) {
                if (response.statusCode != 302) {
                    err_message = util.format("Unexpected response status code %d on Get token request.", response.statusCode);
                    console.error(err_message);
                    deferred.reject(new Error(err_message));
                    return
                }
                if (!response.headers.hasOwnProperty('location')) {
                    err_message = "Location header is not specified in Get token response.";
                    console.error(err_message);
                    deferred.reject(new Error(err_message));
                    return
                }
                location = response.headers.location;

                var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
                var matches = location.match(rePattern);
                if (matches == null) {
                    err_message = util.format("'access_token' parameter not found in location header '%s'", location);
                    console.error(err_message);
                    deferred.reject(new Error(err_message));
                    return
                }

                self.access_token = matches[0].substring("access_token=".length);
                var exec_time_ms = new Date() - start;
                console.info("Access token '%s' for user '%s' is received within %dms", self.access_token, username, exec_time_ms);
                self.header.Value = 'Bearer ' + self.access_token;
                deferred.resolve(self);
            })
        });

    });
    return deferred.promise;
};

/**
 * Make new GhapOAuth
 * @returns {GhapOAuth}
 */
module.exports.makeOAuthClient = function(){return new GhapOAuth()};

function getJSESSSIONID_cookie_str(response) {
    var jsessionid_cookie_str = null;
    if (response.headers.hasOwnProperty('set-cookie'))
        response.headers['set-cookie'].forEach(function (cookie_str) {
            if (cookie_str.indexOf('JSESSIONID') > -1)
                jsessionid_cookie_str = cookie_str;
        });
    return jsessionid_cookie_str;
}
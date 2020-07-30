/**
 * Created by Vlad on 14.07.2015.
 */

var my = require('./ghap-lib');
var cfg = require('./ghap-config');
var util = require('util');
var myConsole = require('./my_console');
var Q = require('q');

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var frisbyInstance = frisby.create('MSG prototype');

var frisbyProto = Object.getPrototypeOf(frisbyInstance);

frisbyProto.setTestName = function(test_name){
	this.testName = test_name;
	return this;
};

frisbyProto.saveAuthHeader = function(auth_header){
	this.authHeader = auth_header;
	return this;
};

frisbyProto.corsOn = function(origin){
	this.corsOrigin = origin;
	this.sendOptionsRequest = true;
	return this;
};

frisbyProto.setExpectedStatus = function(expected_status){
	this.expectedStatus = expected_status;
	return this;
};

frisbyProto.setLogMessage = function(){
	this.logMessage = util.format.apply(this, arguments);
	return this;
};

frisbyProto.next = function(cb){
	this.nextCall = cb;
	return this;
};

frisbyProto.onSuccess = function(cb){
	this.onSuccessCall = cb;
	return this;
};

frisbyProto.onError = function(cb){
	this.onErrorCall = cb;
	return this;
};

frisbyProto.getDefer = function(){
	if ( (typeof this.deferred !== 'object' ) ||
		   !this.deferred.hasOwnProperty('promise') ) {
		this.deferred = Q.defer();
		// console.log("Defer created for ghapFrisby test '%s'", this.testName );
	}
	return this.deferred;
};

var frisby_toss = frisbyProto.toss;
frisbyProto.toss = function (retry) {

	if (this.testName || this.logMessage) {
		var log_message = my.endOfLine;
		if (typeof this.logMessage === 'string')
			log_message += this.logMessage;
		else
			log_message += this.testName + ' request';
		log_message += ' - check response status code';
		if (this.current.expects.length > 1)
			log_message += ' and content.';
		else
			log_message += '. [' + (my.stepNum-1) + ']';
		console.log(log_message);
	}

	if (this.sendOptionsRequest)
		this.corsOptionsRequest();
	else
		frisby_toss.apply(this, retry);
};

frisbyProto.corsOptionsRequest = function() {
	var self = this;
	var  options_request = frisby.create(this.testName + ' OPTIONS request')
		.setExpectedStatus(200)
		//.addHeader(this.authHeader.Name, this.authHeader.Value)
		.addHeader('origin', self.corsOrigin)
		.options(this.current.outgoing.uri)
		.after(function (err, response, body){
			console.log('%s response code %d', this.current.itInfo, response.statusCode);
			//console.log(response.headers);
			var rq_method = self.current.itInfo.split(' ',1)[0];
			expect(response.headers['access-control-allow-origin']).toBe(self.corsOrigin);
			expect(response.headers['access-control-allow-methods']).toContain(rq_method);
			expect(response.statusCode).toBe(200);

			if (response.statusCode === 200) {
				// call frisby toss() method of the source request
				frisby_toss.apply(self);
			} else {
				console.log('Original request cancelled.')
			}
		});

	// call frisby toss() method for OPTIONS request
	frisby_toss.apply(options_request);
};

/**
 *
 * @returns {Promise}
 */
frisbyProto.returnPromise = function(){
	this.toss();
	return this.getDefer().promise;
};


module.exports.create = function(auth_header, test_name, expected_status){

	if ( (typeof auth_header !== 'object') ||
		   !auth_header.hasOwnProperty('Name') ||
		   !auth_header.hasOwnProperty('Value') ) {
		console.error("Invalid authorization header %s in test '%s'", my.getInspectObjStr(auth_header), test_name);
	}

	if (!expected_status) expected_status = 200;

	var start = new Date();
	var ghap_frisby = frisby.create(my.getStepNumStr()+' '+test_name)
		.saveAuthHeader(auth_header)
		.setTestName(test_name)
		.setExpectedStatus(expected_status)
		.addHeader(auth_header.Name, auth_header.Value)
        .after(function (err, response, body) {

            var response_time = this.current.response.time;
            my.logExecutionTime(response_time);

            var deferred = this.getDefer();

            if (err) console.error(err);

            var status_code_info;
            if (response) {
                status_code_info = response.statusCode.toString();
                if (response.statusMessage)
                    status_code_info += '(' + response.statusMessage + ')';
            } else {
                status_code_info = "'is not provided'";
            }

            var response_status = this.current.response.status;
            var resolved_value = response_status;

            if (response_status == this.expectedStatus) {
                console.put('Response time %d ms, response code %s', response_time, status_code_info);
                if (typeof this.onSuccessCall === 'function') {
                    resolved_value = this.onSuccessCall(body);
                }
            } else {
                var err_is_handled = false;
                if (typeof this.onErrorCall === 'function') {
                    err_is_handled = this.onErrorCall(response_status, body);
                }
                if (!err_is_handled) {
                    expect(response_status).toEqual(this.expectedStatus); // to generate test fail
                    var log_message;
                    if (response_status) {
                        log_message = util.format("Unexpected status code %s in '%s' request.", status_code_info, test_name);
                        if (response && response.request && response.request.hasOwnProperty('href')) {
                            log_message += util.format("\nRequest: %s %s", response.request.method, response.request.href)
                        }
                    } else
                        log_message = util.format("No response received in handler after '%s' request", test_name);
                    if (body) log_message += ' Body:';
                    console.log(log_message);
                    if (body) console.log(my.getInspectObjStr(body));
                }
            }

            var err_count = jasmine.getEnv().currentSpec.results().failedCount;
            if (err_count === 0) {
                deferred.resolve(resolved_value);
            } else {
                var err_msg = util.format("%d errors occurred in '%s' test", err_count, test_name);
                deferred.reject(new Error(err_msg));
            }
            if (typeof this.nextCall === 'function')
                this.nextCall(err_count);
            else
            // make short delay to allow promise to be fulfilled
                waits(300);

        });

    return ghap_frisby;
};
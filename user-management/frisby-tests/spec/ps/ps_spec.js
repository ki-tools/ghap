var my = require('../ums/ums_common');

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var ctrlFlow = require('../ums/control_flow');

var testerUser = require('../ums/tester_admin');
var userCRUD = require('./../ums/ums_user_crud');

var psUrls = require('./ps_urls');
var psStorage = require('./ps_resources');

var allVPGs = [];

var asRequests = require('./../as/as_requests');
var allActivities = [];

var oAuth = require('./../oauth/oauth_service2');
oAuth.login(testerUser.getName(), testerUser.getPassword())
	.then(getUserData);

function getUserData() {
	userCRUD.getUser(oAuth.header, testerUser, runSuite)
}

function runSuite () {
	var testerStorage = psStorage.makeStorage(testerUser,1);
	var testerVPG = psStorage.makeVPG(testerUser);

	ctrlFlow.series([
		//function(next) {asRequests.getAllActivities(oAuth.header, allActivities, next)},
		//function(next) {console.log(allActivities); next(); },
		function(next) {allVPG(oAuth.header, allVPGs, next); },
		function(next) {createVPG(oAuth.header, testerVPG, next); },
		function(next) {existsVPG(oAuth.header, testerVPG, next); },
		function(next) {terminateVPG(oAuth.header, testerVPG, next); },
		//function(next) {createStorage(oAuth.header, testerStorage, next); },
		//function(next) {getStorage(oAuth.header, testerStorage, next); },
		//function(next) {existsStorage(oAuth.header, testerStorage, next); },
		//function(next) {deleteStorage(oAuth.header, testerStorage, next); }
	], function(){});
}

function createStorage(authHeader, ps_storage, callback){
	const EXPECTED_STATUS = 200;

	console.log(ps_storage.getCreateStorage_json());
	frisby.create(my.getStepNumStr()+' Create Personal Storage')
		.put(psUrls.getCreateStorage_Url(ps_storage.uuid, ps_storage.g_size))
		//.put(psUrls.getCreateStorage_Url(),
		//  ps_storage.getCreateStorage_json(), {json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create Personal Storage - check response status code.');
			console.log(body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function getStorage(authHeader, ps_storage, callback){
	const EXPECTED_STATUS = 200;
	frisby.create(my.getStepNumStr()+' Get Personal Storage Data')
		.get(psUrls.getGetStorage_Url(ps_storage.uuid))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get Personal Storage - check response status code.');
			console.log(body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function existsStorage(authHeader, ps_storage, callback){
	const EXPECTED_STATUS = 200;
	frisby.create(my.getStepNumStr()+' Check If Personal Storage Exists')
		.get(psUrls.getExistsStorage_Url(ps_storage.uuid))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Check if Personal Storage exists - check response status code.');
			console.log(body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function deleteStorage(authHeader, ps_storage, callback){
	const EXPECTED_STATUS = 200;
	frisby.create(my.getStepNumStr()+' Delete Personal Storage Data')
		.get(psUrls.getDeleteStorage_Url(ps_storage.uuid))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete Personal Storage - check response status code.');
			console.log(body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

//----------------------------------------------------------------

function createVPG(authHeader, ps_vpg, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create VPG')
		.put(psUrls.getCreateVPG_Url(ps_vpg.userId),
		  ps_vpg.getCreateVPG_json(), {json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create VPG - check response status code.');
			my.logExecutionTime(start);

			if (this.current.response.status == EXPECTED_STATUS) {
				expect(typeof body).toBe('object');
				if (typeof body === 'object') {
					ps_vpg.id = body.id;
					ps_vpg.activityId = body.activityId;
					ps_vpg.stackId = body.stackId;
					ps_vpg.pemKey = body.pemKey;
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function existsVPG(authHeader, ps_vpg, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Check If VPG Exists')
		.get(psUrls.getExistsVPG_Url(ps_vpg.userId))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Check if VPG exists - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function terminateVPG(authHeader, ps_vpg, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Terminate VPG')
		.delete(psUrls.getTerminateVPG_Url(ps_vpg.userId))
		//.delete(psUrls.getTerminateVPG_Url('194eb918-4d8a-4755-bde1-18ed51b69456'))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Terminate VPG - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function allVPG(authHeader, allVPG_array, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Get All VPG')
		.get(psUrls.getAllVPG_Url())
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get All VPG - check response status code.');
			my.logExecutionTime(start);

			var response_status = this.current.response.status;
			if (response_status == 200) {
				var parsed_array = my.jsonParse(body);
				if (parsed_array instanceof Array) {
					var element = parsed_array.shift();
					while(element) {
						allVPG_array.push( element  );
						element = parsed_array.shift();
					}
					//console.log(allVPG_array);
				}
			} else
				console.log ('Unexpected status code '+response_status+'. Body:'+my.endOfLine+body);


			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

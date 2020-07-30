var my = require('./../Common/ghap-lib');
var endOfLine = my.endOfLine;
my.stepPrefix = 'UserCRUD';
my.logModuleName(module.filename);

var cfg = require('./../Common/ghap-config');

var umsUser = require('./ums_user');
var umsRequests = require('./ums_requests');
var Tester = umsUser.makeUser('MrTester');

var frisby = require('frisby');
var umsUrls = require('./ums_urls');

// Load the AWS SDK for Node.js
var AWS = require('aws-sdk');

AWS.config.loadFromPath('./spec/ums/aws_config.json');

var ctrlFlow = require('./../Common/control_flow');

var useOAuth = true;
var oAuth;
var authHeader = {
	Name: '',
	Value: ''
};

// START
(function () {
	if (useOAuth) {
		var oAuthService = require('./../oauth/oauth_service2');
		oAuth = oAuthService.makeOAuthClient();
		var testerAdmin = require('./tester_admin');
		oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
			.then(runSuiteWithOAuth);
	} else {
		var doAdminSignIn = require('./ums_AdminSignIn');
		doAdminSignIn(runSuiteWithCookieAuth);
	}
}());

function runSuiteWithCookieAuth(cookie_str) {
	authHeader.Name = 'Cookie';
	authHeader.Value = cookie_str;
	runSuite();
}

function runSuiteWithOAuth() {
	authHeader.Name = 'Authorization';
	authHeader.Value = oAuth.access_token;
	runSuite();
}

function runSuite(){
	umsRequests.createUser(authHeader, Tester, function(create_err_count){
		var calls = [];

		if (create_err_count == 0) {
			calls.push(function (next) {tryToCreateExistingUser(next);	});
			calls.push(function (next) {umsRequests.getUser(authHeader, Tester, next)	});
			calls.push(function (next) {umsRequests.updateUser(authHeader, Tester, {email:"aaaa@bbbb.ccc", firstName: "TESTAM"}, next);	});
			calls.push(function (next) {
				var testerAdmin = require('./tester_admin');
				umsRequests.getUserRoles(authHeader, testerAdmin, next)
			});
		}

		calls.push(	function(next) {umsRequests.deleteUser( authHeader, Tester, next ); });

		ctrlFlow.series(calls, function(){console.log('Test suite is finished.')});

	});
}

function tryToCreateExistingUser (callback) {

	var expected_json = Tester.getUser_ExpectedJson();

	delete expected_json.guid;
	expected_json.errors = [{field: 'name'}];

	frisby.create(my.getStepNumStr()+' Try to Create Existing User')
		.post(umsUrls.getCreateUser_Url(),Tester.getCreate_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(400)
		.expectJSON(expected_json)
		.after(function (err, response, body) {
			console.log(endOfLine + 'Try to create existing User - check response status code and content.');

			expect(body.errors).toBeDefined();

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
}

// deprecated
// cfg.usersS3Bucket = "userscratch-us-east-1-qa"
function checkS3bucket( guid, callback) {
	// Configuring the SDK in Node.js
	// http://docs.aws.amazon.com/AWSJavaScriptSDK/guide/node-configuring.html
	self = this;
	var s3 = new AWS.S3();
	var s3_params = {
		Bucket: cfg.usersS3Bucket,
		Prefix: 'users/'+guid
	};
	var s3RequestFinished = false;

	console.log("\nObject with quid '%s' requested from S3.", guid);
	s3.listObjects(s3_params, function(err, data) {
		console.log('S3 response received.');

		expect(err).toBeNull();
		if (err) {
			console.log("S3 error:", err);
			console.log(s3_params);
		}

		expect(data).not.toBeNull();
		if (data) {
			//for (var index in data.Contents)
			//	console.log( data.Contents[index].Key );
			expect(data.Contents.length).toBeGreaterThan(0)
			if (data.Contents.length === 0)
				console.log("Folder '%s' not found in bucket '%s'", s3_params.Prefix, s3_params.Bucket);
		} else
			console.log("No objects found in bucket '%s' for folder '%s'", s3_params.Bucket, s3_params.Prefix);


		s3RequestFinished = true;
		callback(true);
	});

	// call waitsFor to prevent termination of jasmine
	waitsFor(function(){return s3RequestFinished;	}
		,"S3 bucket request timed out before completing", 5000)
}
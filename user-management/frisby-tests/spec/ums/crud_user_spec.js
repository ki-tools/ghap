var my = require('./ums_common');
var endOfLine = my.endOfLine;

var umsUser = require('./ums_user');
var Tester = umsUser.makeUser('MrTester');

var frisby = require('frisby');
var umsUrls = require('./ums_urls');

// Load the AWS SDK for Node.js
var AWS = require('aws-sdk');

// Set your region for future requests.
AWS.config.region = 'us-east-1';

var ctrlFlow = require('./control_flow');

var useOAuth = true;
var oAuth;
var authHeader = {
	Name: '',
	Value: ''
};

// START
(function () {
	if (useOAuth) {
		oAuth = require('./ums_oauth');
		oAuth.waitAccessToken(runSuiteWithOAuth);
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
	my.stepPrefix = 'U';
	ctrlFlow.series([ function(next) {createNewUser( next ); }]
		, checkBucket);
}

function createNewUser(callback) {

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create User')
		.timeout(40000)
		.post(umsUrls.getCreateUser_Url(), Tester.getCreate_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(Tester.getUser_json())
		.after(function (err, response, body) {
			console.log(endOfLine + 'Create User '+Tester.getName()+' - check response status code and content.');

			var exec_time_ms = new Date() - start;
			if (exec_time_ms > 5000)
				console.info("WARNING: Execution time is too long: %dms", exec_time_ms);

			var responseStatus = this.current.response.status;
			if (responseStatus == 400) {
				var body_json = my.jsonParse(body);
				if (body_json.hasOwnProperty('errors')) {
					console.log('>> errors >>');
					console.log(JSON.stringify(body_json.errors));
				}
			}
			callback(responseStatus, body);
		})
		.toss();
}

function checkBucket(create_user_results){
	var calls = [];
	var response_status = create_user_results[0][0];

	if (response_status == 200) {
		var guid = create_user_results[0][1].guid;
		calls.push(function (next) {checkS3bucket(guid, next);	});
		calls.push(function (next) {tryToCreateExistingUser(next);	});
		calls.push(function (next) {findUser(next);	});
		calls.push(function (next) {updateUserEmail(next);	});
		calls.push(function (next) {updateUserFirstName(next);	});
	}

	calls.push(	function(next) {deleteExistingUser( next ); }	);

	ctrlFlow.series(calls, function(){console.log('Test suite is finished.')});
}

function tryToCreateExistingUser (callback) {

	var expected_json = Tester.getUser_json('cn');
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

function findUser(callback) {
	frisby.create(my.getStepNumStr() + ' Find Existing User')
		.get( umsUrls.getUser_Url(Tester.getDn()) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(Tester.getUser_json())
		.after(function (err, response, body) {
			console.log(endOfLine + 'Find existing user - check response status and content.');
			// console.log(body);

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
}

function updateUserEmail(callback) {
	Tester.setEmail("testam@test.com");
	frisby.create(my.getStepNumStr() + ' Update Email For Existing User')
		.post(
		  umsUrls.getUser_Url( Tester.getDn() ),
	  	{
			  email: "testam@test.com"
		  },
		  {json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(Tester.getUser_json())
		.after(function (err, response, body) {
			console.log(endOfLine + 'Update user`s email - check response status and content.');
			// console.log(body);

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
}

function updateUserFirstName(callback) {

	var tester_dn = Tester.getDn();

	//var old_firstName = Tester.getUser_json().firstName;
	var new_firstName = 'Testam';
	Tester.setFirstName(new_firstName);

	frisby.create(my.getStepNumStr() + ' Update FirstName For Existing User')
		.post(
		umsUrls.getUser_Url(tester_dn),
		{
			firstName: new_firstName
		},
		{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(Tester.getUser_json('cn'))
		.after(function (err, response, body) {
			console.log(endOfLine + 'Update user`s first name - check response status and content.');
			// console.log(body);

			//var responseStatus = this.current.response.status;
			//if (responseStatus != 200) Tester.setFirstName(old_firstName);

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
}

function deleteExistingUser(callback) {
	var test = frisby.create(my.getStepNumStr() + ' Delete Existing User')
		.delete(umsUrls.getUser_Url( Tester.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(endOfLine + 'Delete existing user - check response status.');
			// console.log(body);

			callback( jasmine.getEnv().currentSpec.results().failedCount );
		});
	test.toss();
}

function checkS3bucket(guid,callback) {
	self = this;
	var s3 = new AWS.S3();
	var s3_params = {
		Bucket: 'userscratch-us-east-1-dev',
		Prefix: 'users/'+guid.toString()
	};
	var s3RequestFinished = false;

	console.log('Object with quid '+guid+' requested from S3.');
	s3.listObjects(s3_params, function(err, data) {
		console.log('S3 response received.');
		expect(data).not.toBeNull();
		if (err) {
			console.log("S3 error:", err);
		}
		else {
			//for (var index in data.Contents)
			//	console.log( data.Contents[index].Key );
			expect(data.Contents.length).toEqual(1)
		}
		s3RequestFinished = true;
		callback(true);
	});

	// call waitsFor to prevent termination of jasmine
	waitsFor(function(){return s3RequestFinished;	}
		,"S3 bucket request timed out before completing", 5000)
}
var my = require('../ums/ums_common');

var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var ctrlFlow = require('../ums/control_flow');

var testerUser = require('../ums/tester_admin');
var userCRUD = require('../ums/ums_user_crud');

var allProjects = [];
var allGrants = [];
var prjUrls = require('./prj-prov_urls');
var prjResources = require('./prj-prov_resources');
var testPrj = prjResources.makeProject('TestProject','TestProjectKey','TestProject description');
var testGrant = prjResources.makeGrant('Grant1');

var oAuth = require('./../oauth/oauth_service2');
oAuth.login(testerUser.getName(), testerUser.getPassword())
	.then(getUserData);

function getUserData() {
	userCRUD.getUser(oAuth.header, testerUser, runSuite)
}

function runSuite () {
	ctrlFlow.series([
		function(next) {createProject(oAuth.header, testPrj, next); },
		function(next) {createGrant(oAuth.header, testPrj, testGrant, next); },
		//function(next) {getAllProjects(oAuth.header, allProjects, next); },
		//function(next) {getAllGrants(oAuth.header, testPrj, function(){
		//	console.log(testPrj.grants);
		//	next();
		//}); },
		function(next) {deleteGrant(oAuth.header, testGrant, next); },
		//function(next) {deleteProjects(oAuth.header, allProjects, next); }
		function(next) {deleteProject(oAuth.header, testPrj, next); }
	], final);

}

function getAllProjects(authHeader, all_projects, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Get All Projects')
		.get(prjUrls.getAllProjects_Url())
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get All Projects - check response status code.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				var parsed_array = my.jsonParse(body);
				if (parsed_array instanceof Array) {
					var element = parsed_array.shift();
					while(element) {
						var prj_res =  prjResources.makeProject();
						my.copyProperties(element,prj_res);
						all_projects.push( prj_res );
						element = parsed_array.shift();
					}
					console.log(all_projects);
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function createProject(authHeader, prj_resource, callback) {
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create Project')
		.post(prjUrls.getCreateProject_Url(),
		  prj_resource.getCreateProject_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create Project '+ prj_resource.name +' - check response status code.');
			my.logExecutionTime(start);

			if(this.current.response.status == EXPECTED_STATUS) {
				expect(typeof body).toBe('object');
				my.copyProperties(body, prj_resource);
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function deleteProject(authHeader, prj_resource, callback) {
	const EXPECTED_STATUS = 204;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Delete Project')
		.delete(prjUrls.getDeleteProject_Url(prj_resource.id),
		prj_resource.getCreateProject_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete Project ' + prj_resource.name + ' - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function getAllGrants(authHeader, prj_resource, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Get All Grants')
		.get(prjUrls.getAllGrants_Url(prj_resource.id))
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get All Grants - check response status code.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				var parsed_array = my.jsonParse(body);
				if (parsed_array instanceof Array) {
					prj_resource.grants.length = 0;
					var element = parsed_array.shift();
					while(element) {
						var grant_res =  prjResources.makeGrant();
						my.copyProperties(element,grant_res);
						prj_resource.addGrant( grant_res );
						element = parsed_array.shift();
					}
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function createGrant(authHeader, prj_res, grant_res, callback) {
	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create Grant')
		.post(prjUrls.getCreateGrant_Url(prj_res.id),
		grant_res.getCreateGrant_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create Grant '+ grant_res.name + ' in project ' + prj_res.name + ' - check response status code.');
			my.logExecutionTime(start);
			console.log(body);

			if(this.current.response.status == EXPECTED_STATUS) {
				expect(typeof body).toBe('object');
				my.copyProperties(body, grant_res);
				prj_res.addGrant(grant_res)
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}

function deleteGrant(authHeader, grant_res, callback) {
	const EXPECTED_STATUS = 204;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Delete Grant')
		.delete(prjUrls.getDeleteGrant_Url(grant_res.id))
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete Grant ' + grant_res.name + ' - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
}


function deleteProjects(authHeader, projects_array, callback){
	var delete_calls = [];
	projects_array.forEach(function(prj_res){
		delete_calls.push(
			function (next) {
				deleteProject(authHeader, prj_res, next);
			}
		);
	});
	ctrlFlow.series(delete_calls, callback);
}

function final(){
	console.log(testPrj.grants);
}
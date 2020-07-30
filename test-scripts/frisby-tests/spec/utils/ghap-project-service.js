var Qs = require('qs');
var request = require('request');
var util = require('util');

var my = require('../Common/ghap-lib');
var prjUrls = require('../prj-prov/prj-prov_urls');
var prjResources = require('../prj-prov/prj-prov_resources');

var headers = {};

exports.createProject = function(authHeader, prj_resource, callback) {
	const EXPECTED_STATUS = 200;
	//console.log(prjUrls.getCreateProject_Url());
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getCreateProject_Url(),
		method: 'POST',
		json: true,
		headers: headers,
		body: prj_resource.getCreateProject_json()
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Create Project '%s'",prj_resource.name),
			status_code: 0
		};
		if (error){
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS){
				result.is_successful = true;
				my.copyProperties(body, prj_resource);
			} else
				console.log(body);
		}
		callback(result);
	});
};

exports.deleteProject = function(authHeader, prj_resource, callback) {
	const EXPECTED_STATUS = 204;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getDeleteProject_Url(prj_resource.id),
		method: 'DELETE',
		json: true,
		headers: headers
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Delete Project '%s'",prj_resource.name),
			status_code: 0
		};
		if (error){
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS)
				result.is_successful = true;
			else
				console.log(body);

		}
		callback(result);
	});
};

exports.createGrant = function (authHeader, prj_res, grant_res, callback) {
	const EXPECTED_STATUS = 200;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getCreateGrant_Url(prj_res.id),
		method: 'POST',
		json: true,
		headers: headers,
		body: grant_res.getCreateGrant_json()
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Create grant '%s' in project '%s'",grant_res.name, prj_res.name),
			status_code: 0
		};
		if (error){
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS)
				result.is_successful = true;
			else
				console.log(body);

		}
		callback(result);
	});
};

exports.deleteGrant = function(authHeader, grant_resource, callback) {
	const EXPECTED_STATUS = 204;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getDeleteGrant_Url(grant_resource.id),
		method: 'DELETE',
		json: true,
		headers: headers
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Delete Grant '%s'",grant_resource.name),
			status_code: 0
		};
		if (error){
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS)
				result.is_successful = true;
			else
				console.log(body);
		}
		callback(result);
	});
};

exports.getAllProjects = function (authHeader, all_projects, callback) {
	const EXPECTED_STATUS = 200;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getAllProjects_Url(),
		method: 'GET',
		headers: headers
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Get All Projects"),
			status_code: 0
		};
		if (error) {
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS) {
				result.is_successful = true;
				var parsed_array = my.jsonParse(body);
				if (parsed_array instanceof Array) {
					var element = parsed_array.shift();
					while (element) {
						var prj_res = prjResources.makeProject();
						my.copyProperties(element, prj_res);
						all_projects.push(prj_res);
						element = parsed_array.shift();
					}
				}
			} else
				console.log(body);
		}
		callback(result);
	});
};

exports.getAllGrants = function (authHeader, prj_res, callback) {
	const EXPECTED_STATUS = 200;
	headers['content-type'] = 'application/json';
	headers[authHeader.Name] = authHeader.Value;
	request({
		url: prjUrls.getAllGrants_Url(prj_res.id),
		method: 'GET',
		headers: headers
	}, function (error, response, body) {
		var result = {
			is_successful: false,
			message: util.format("Get All Grants for project '%s'", prj_res.name),
			status_code: 0
		};
		if (error) {
			result.message += ' error.';
			console.log(result.message);
			console.error(error)
		} else {
			result.status_code = response.statusCode;
			result.message += util.format(". Response status code %d", response.statusCode);
			console.log(result.message);
			if (response.statusCode == EXPECTED_STATUS) {
				result.is_successful = true;
				var parsed_array = my.jsonParse(body);
				prj_res.grants.length = 0;
				if (parsed_array instanceof Array) {
					var element = parsed_array.shift();
					while (element) {
						var grant_res = prjResources.makeGrant();
						my.copyProperties(element, grant_res);
						prj_res.addGrant(grant_res);
						element = parsed_array.shift();
					}
				}
			} else
				console.log(body);
		}
		callback(result);
	});
};

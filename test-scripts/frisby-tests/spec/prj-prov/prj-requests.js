/**
 * Created by Vlad on 12.06.2015.
 */

var my = require('./../Common/ghap-lib');

var ghapFrisby = require('./../Common/ghap-frisby');

var prjUrls = require('./prj-prov_urls');
var prjResources = require('./prj-prov_resources');

module.exports.createProject = function(authHeader, prj_resource, callback){
	const EXPECTED_STATUS = 200;

	var start = new Date();
	return ghapFrisby.create(authHeader, ' Create Project', EXPECTED_STATUS)
		.post(prjUrls.getCreateProject_Url(),	prj_resource.getCreateProject_json(),{json: true})
		.setLogMessage("Create project '%s'", prj_resource.name)
		.onSuccess(function (body) {
			expect(typeof body).toBe('object');
			my.copyProperties(body, prj_resource);
		})
		.next(callback)
		.returnPromise();
};

module.exports.deleteProject = function(authHeader, prj_resource, callback) {
	const EXPECTED_STATUS = 204;

	return ghapFrisby.create(authHeader, 'Delete Project', EXPECTED_STATUS)
		.delete(prjUrls.getDeleteProject_Url(prj_resource.id))
		.addHeader('content-type', 'application/json')
		.setLogMessage("Delete project '%s'", prj_resource.name)
		.next(callback)
		.returnPromise();
};

module.exports.getAllProjects = function(authHeader, all_projects, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Projects', EXPECTED_STATUS)
		.get(prjUrls.getAllProjects_Url())
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
			var parsed_array = my.jsonParse(body);
			expect(parsed_array instanceof Array).toBe(true);

			if (parsed_array instanceof Array) {
				all_projects.length = 0;
				var element = parsed_array.shift();
				while(element) {
					var prj_res =  prjResources.makeProject();
					my.copyProperties(element, prj_res);
					all_projects.push( prj_res );
					element = parsed_array.shift();
				}
			}

		})
		.next(callback)
		.returnPromise();
};

module.exports.getAllProjects4User = function(authHeader, ums_user, callback, error_handler){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Projects For User', EXPECTED_STATUS)
			.get(prjUrls.getAllProjects4User_Url(ums_user.getGuid()))
			.addHeader('content-type', 'application/json')
			.setLogMessage("Get All Projects For User '%s'", ums_user.getName())
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);
				my.moveArray(parsed_array,ums_user.projects);
			})
			.onError(function(response_status, body){
				if(typeof error_handler === 'function') return error_handler(response_status, body);
				return false;
			})
			.next(callback)
			.returnPromise();
};

module.exports.getAllGrantsOfProject4User = function(authHeader, prj, ums_user){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Grants Of Project For User', EXPECTED_STATUS)
			.get(prjUrls.getAllGrantsOfProject4User_Url(prj.id, ums_user.getGuid()))
			.addHeader('content-type', 'application/json')
			.setLogMessage("Get All Grants of Project '%s' For User '%s'", prj.name, ums_user.getName())
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);
				return parsed_array;
			})
			.returnPromise();
};

function UmsEntry(ums_object){
	if (ums_object.hasOwnProperty('objectClass') && (ums_object.objectClass === 'group')) {
		this.guid = ums_object.guid;
		this.name = 'group ' + ums_object.name;
	} else {
		this.guid = ums_object.getGuid();
		this.name = 'user ' + ums_object.getName();
	}
}

/**
 * @param {GhapAuthHeader} authHeader
 * @param {Object} prj_res
 * @param {Object} ums_object - UmsUser or UmsGroup
 * @param {string[]} permissions
 * @param {function} [callback]
 */
module.exports.grantPermissionsOnProject = function(authHeader, prj_res, ums_object, permissions, callback){
	const EXPECTED_STATUS = 200;

	var ums_entry = new UmsEntry(ums_object);

	return ghapFrisby.create(authHeader, 'Grant Project Permissions', EXPECTED_STATUS)
		.post( prjUrls.getGrantProjectPermissions_Url(prj_res.id, ums_entry.guid), permissions, {json: true} )
		.setLogMessage("Grant [%s] permissions to program '%s' for %s",  permissions.toString(), prj_res.name, ums_entry.name)
		.next(callback)
		.returnPromise();
};

/**
 * @param {GhapAuthHeader} authHeader
 * @param {Object} prj_res
 * @param {Object} ums_object - UmsUser or UmsGroup
 * @param {string[]} permissions
 * @param {function} [callback]
 */
module.exports.revokeProjectPermissions = function(authHeader, prj_res, ums_object, permissions,  callback){
	const EXPECTED_STATUS = 200;

	var ums_entry = new UmsEntry(ums_object);

	return ghapFrisby.create(authHeader, 'Grant Project Permissions', EXPECTED_STATUS)
		.post( prjUrls.getRevokeProjectPermissions_Url(prj_res.id, ums_entry.guid), permissions, {json: true} )
		.setLogMessage("Revoke [%s] permissions from program '%s' for %s",  permissions.toString(), prj_res.name, ums_entry.name)
		.next(callback)
		.returnPromise();
};


module.exports.createGrant = function(authHeader, prj_res, grant_res, callback) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader,' Create Grant', EXPECTED_STATUS)
		.post(prjUrls.getCreateGrant_Url(prj_res.id),	grant_res.getCreateGrant_json(), {json: true})
		//.addHeader('content-type', 'application/json')
		.setLogMessage("Create grant '%s' in project '%s'", grant_res.name, prj_res.name)
		.onSuccess(function(body){
			expect(typeof body).toBe('object');
			my.copyProperties(body, grant_res);
			prj_res.addGrant(grant_res)
		})
		.next(callback)
		.returnPromise();

};

module.exports.deleteGrant = function(authHeader, grant_res, callback) {
	const EXPECTED_STATUS = 204;

    return ghapFrisby.create(authHeader,' Delete Grant', EXPECTED_STATUS)
        .delete(prjUrls.getDeleteGrant_Url(grant_res.id))
        .addHeader('content-type', 'application/json')
        .setLogMessage("Delete grant '%s' in project '%s'", grant_res.name)
        .next(callback)
        .returnPromise();

/*	var start = new Date();
	frisby.create(my.getStepNumStr()+' Delete Grant')
		.delete(prjUrls.getDeleteGrant_Url(grant_res.id))
		.addHeader(authHeader.Name, authHeader.Value)
		.addHeader('content-type', 'application/json')
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete Grant ' + grant_res.name + ' - check response status code.');
			my.logExecutionTime(start);

			if (err) console.log(err);

			var response_status = this.current.response.status;
			if(response_status != EXPECTED_STATUS)
				console.log ("Unexpected status code '%d' in Delete Grant request. Body:\n<%s>", response_status, body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();*/
};

/**
 *
 * @param {GhapAuthHeader} authHeader
 * @param {Object} prj_resource
 * @param {Function} [callback]
 */
module.exports.getAllGrants = function(authHeader, prj_resource, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Grants', EXPECTED_STATUS)
		.get(prjUrls.getAllGrants_Url(prj_resource.id))
		.addHeader('content-type', 'application/json')
		.setLogMessage("Get all grants for program '%s'", prj_resource.name)
		.onSuccess(function (body) {
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
		})
		.next(callback)
		.returnPromise();
};

module.exports.getAllUsers4Grant = function(authHeader, grant_res, all_grant_users, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Grants', EXPECTED_STATUS)
		.get(prjUrls.getAllUsers4Grant_Url(grant_res.id))
		.addHeader('content-type', 'application/json')
		.setLogMessage("Get all users for grant '%s'", grant_res.name)
		.onSuccess(function (body) {
			var parsed_array = my.jsonParse(body);
			expect(parsed_array instanceof Array).toBe(true);
			my.moveArray(parsed_array, all_grant_users);
		})
		.next(callback)
		.returnPromise();
};


module.exports.grantPermissionsOnGrant = function(authHeader, grant_res, ums_user, permissions, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Grant Grant Permissions', EXPECTED_STATUS)
		.post(prjUrls.getGrantGrantPermissions_Url(grant_res.id, ums_user.getGuid()),	permissions,{json: true})
		.setLogMessage("Grant [%s] permissions to grant '%s' for user '%s'",
		    permissions.toString(), grant_res.name, ums_user.getName())
		.next(callback)
		.returnPromise();
};

module.exports.revokeGrantPermissions = function(authHeader, grant_res, ums_user, permissions, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Grant Grant Permissions', EXPECTED_STATUS)
		.post(prjUrls.getRevokeGrantPermissions_Url(grant_res.id, ums_user.getGuid()),	permissions,{json: true})
		.setLogMessage("Revoke [%s] permissions from grant '%s' for user '%s'",
		    permissions.toString(), grant_res.name, ums_user.getName())
		.next(callback)
		.returnPromise();
};

module.exports.getProjectUsersPermissions = function(authHeader, project, all_users){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Project Users Permissions', EXPECTED_STATUS)
			.get(prjUrls.getProjectUsersPermissions_Url(project.id))
			.setLogMessage("Get users permissions for project '%s'", project.name)
			.addHeader('content-type', 'application/json')
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);

				if (parsed_array instanceof Array) {
					project.permissions = parsed_array.map(function(obj){
						var user_name = null;
						if (all_users) {
							var user = my.findElementInArray(all_users, 'guid', obj.guid);
							if (user) user_name = user.name;
						}
						return {
							"guid" : obj.guid,
							"username" : user_name,
							"permission" : obj.permissions.toString()
						}
					});
				}

			})
			.returnPromise();
};

module.exports.getGrantUsersPermissions = function(authHeader, grant, all_users){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Grant Users Permissions', EXPECTED_STATUS)
			.get(prjUrls.getAllUsers4Grant_Url( grant.id))
			.setLogMessage("Get users permissions for grant '%s'", grant.name)
			.addHeader('content-type', 'application/json')
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);

				if (parsed_array instanceof Array) {
					grant.permissions = parsed_array.map(function(obj){
						var user_name = null;
						if (all_users) {
							var user = my.findElementInArray(all_users, 'guid', obj.guid);
							if (user) user_name = user.name;
						}
						return {
							"guid" : obj.guid,
							"username" : user_name,
							"permission" : obj.permissions.toString()
						}
					});
				}

			})
			.returnPromise();
};

/*------------------------------------------------------------------------------------------------------
DIRECT STASH REQUESTS
--------------------------------------------------------------------------------------------------------*/

/*  @Path("/get") - get all projects */

 module.exports.getAllStashProjects = function(authHeader, all_stash_projects, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Stash Projects', EXPECTED_STATUS)
		.get(prjUrls.getAllStashProjects_Url())
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
			var parsed_array = my.jsonParse(body);
			expect(parsed_array instanceof Array).toBe(true);

			if (parsed_array instanceof Array) {
				my.moveArray(parsed_array, all_stash_projects);
			}

		})
		.next(callback)
		.returnPromise();
};

/*
 @Path("/get/
 {projectKey}") - get all grants
 */
module.exports.getAllStashGrants4Project = function(authHeader, stash_project, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Stash Grants For ' + stash_project.key + ' project', EXPECTED_STATUS)
		.get(prjUrls.getAllStashGrants4Project_Url(stash_project.key))
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
			var parsed_array = my.jsonParse(body);
			expect(parsed_array instanceof Array).toBe(true);

			if (parsed_array instanceof Array) {
				stash_project.grants = [];
				my.moveArray(parsed_array, stash_project.grants);
			}

		})
		.next(callback)
		.returnPromise();
};

/*  @Path("/get/{projectKey}/permissions") */

module.exports.getStashProjectUsersPermissions = function(authHeader, stash_project){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Stash Project Permissions', EXPECTED_STATUS)
			.get(prjUrls.getStashProjectUsersPermissions_Url(stash_project.key))
			.setLogMessage("Get Stash permissions for Project '%s'", stash_project.key)
			.addHeader('content-type', 'application/json')
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);

				if (parsed_array instanceof Array) {
					stash_project.permissions = parsed_array.map(function(obj){
						return {
							"username" : obj.user.name,
							"permission" : obj.permission
						}
					});
				}

			})
			.returnPromise();
};

/*  @Path("/get/{projectKey}/permissions/repo/{slug}") */

module.exports.getStashGrantUsersPermissions = function(authHeader, stash_project, stash_grant){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Stash Grant Permissions', EXPECTED_STATUS)
			.get(prjUrls.getStashGrantUsersPermissions_Url( stash_project.key, stash_grant.slug))
			.setLogMessage("Get Stash permissions for Grant '%s/%s'", stash_project.key, stash_grant.name)
			.addHeader('content-type', 'application/json')
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);

				if (parsed_array instanceof Array) {
					stash_grant.permissions = parsed_array.map(function(obj){
						return {
							"username" : obj.user.name,
							"permission" : obj.permission
						}
					});
				}

			})
			.returnPromise();
};

/*
 @Path("/get/{projectKey}
 /permissions/
 {username}") - project permissions for user
 */

/**
 * Get project permissions for the user directly from STASH
 * @param {GhapAuthHeader} authHeader
 * @param {string} stash_project_key
 * @param {string} stash_username
 * @return {Q.Promise} promise that will be resolved with permissions array of string
 */
module.exports.getStashProjectPermissions4User = function(authHeader, stash_project_key, stash_username){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Stash Project Permissions For User', EXPECTED_STATUS)
		.get(prjUrls.getStashProjectPermissions4User_Url(stash_project_key, stash_username))
		.setLogMessage("Get Stash permissions: project '%s' user '%s'", stash_project_key, stash_username)
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
			var parsed_array = my.jsonParse(body);
			expect(parsed_array instanceof Array).toBe(true);
			if (parsed_array instanceof Array) {
				//console.log(body);
				if (parsed_array.length === 0) {
					return '';
				} else {
					expect(parsed_array.length).toBe(1);
					expect(parsed_array[0].user.name).toBe(stash_username);
					return parsed_array[0].permission;
				}
			}
			return null;
		})
		.returnPromise();
};

/* @Path("/get/{projectKey}/permissions/{username}/repo/{slug}") - grant permissions for user */
/*
  Response example:
  [{ "permission":"REPO_READ",
    "user": {
      "name":"A150829-0",
      "emailAddress":"andrew.krasnoff+3@gmail.com",
      "id":29,
      "displayName":"A150829 0",
      "active":true,
      "slug":"a150829-0",
      "type":"NORMAL"
    }
  }]
 */
module.exports.getStashGrantsPermissions4User = function (authHeader, stash_project_key, stash_username, stash_slug) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Stash Grant Permissions For User', EXPECTED_STATUS)
			.get(prjUrls.getStashGrantPermissions4User_Url(stash_project_key, stash_username, stash_slug))
			.setLogMessage("Get Stash permissions: project '%s' user '%s' repo '%s'", stash_project_key,
					stash_username, stash_slug)
			.addHeader('content-type', 'application/json')
			.onSuccess(function (body) {
				var parsed_array = my.jsonParse(body);
				expect(parsed_array instanceof Array).toBe(true);
				if (parsed_array instanceof Array) {
					//console.log(body);
					if (parsed_array.length === 0) {
						return '';
					} else {
						expect(parsed_array.length).toBe(1);
						expect(parsed_array[0].user.name).toBe(stash_username);
						return parsed_array[0].permission;
					}
				}
				return null;
			})
			.returnPromise();
};

module.exports.isFileExistsInStash = function (authHeader, fileName) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Check File In Stash', EXPECTED_STATUS)
		.get(prjUrls.isFileExistsInStash_Url(fileName))
		.setLogMessage("Check if file '%s' exists in Stash master branch", fileName)
		.addHeader('content-type', 'application/json')
		.onSuccess(function (body) {
            console.log(body)
		})
		.returnPromise();
};
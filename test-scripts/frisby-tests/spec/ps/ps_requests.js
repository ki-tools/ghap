/**
 * Created by Vlad on 08.06.2015.
 */

var Q = require('q');

var my = require('./../Common/ghap-lib');

var ghapFrisby = require('./../Common/ghap-frisby');

var psUrls = require('./ps_urls');
var psResources = require('./ps_resources');

/**
 *
 * @param {GhapAuthHeader} authHeader
 * @param {vpgStack[]} allVPG_array - results
 * @param {Function} callback
 */
module.exports.getAllVPGs = function(authHeader, allVPG_array, callback) {
	ghapFrisby.create(authHeader, 'Get All VPG')
		.get(psUrls.getAllVPG_Url())
		.onSuccess(function (body) {
			my.moveArray( my.jsonParse(body), allVPG_array);
		})
		.next(callback)
		.toss();
};

module.exports.createVPG = function(authHeader, ghap_activity, ps_vpg, callback){
	ghapFrisby.create(authHeader, 'Create CE')
		.put(psUrls.getCreateVPG_Url(ps_vpg.userId), ghap_activity, {json: true})
		.setLogMessage("Create CE '%s' for user with id '%s'", ghap_activity.activityName, ps_vpg.userId)
		.onSuccess(function (body) {
			expect(typeof body).toBe('object');
			if (typeof body === 'object') {
				my.copyProperties(body, ps_vpg);
			}
		})
		.next(callback)
		.toss();
};

// id should be defined in ps_vpg at the moment of call
module.exports.getVPG = function(authHeader, ps_vpg, callback) {
	ghapFrisby.create(authHeader, 'Check If CE Exists')
		.get(psUrls.getExistsVPG_Url(ps_vpg.userId))
		.setLogMessage("Check if CE for user with id '%s' exists", ps_vpg.userId)
		.onSuccess(function (body) {
			this.next(null); // prevent callback immediately after onSuccess event
			ghapFrisby.create(authHeader, 'Get CE info')
				.get(psUrls.getVPG_Url(ps_vpg.userId))
				.onSuccess(function (body) {
					this.next(null); // prevent callback immediately after onSuccess event
					var received_vpg = my.jsonParse(body);
					expect(received_vpg.userId).toEqual(ps_vpg.userId);
					my.copyProperties(received_vpg, ps_vpg);
					ghapFrisby.create(authHeader, 'Get CE compute resources')
						.get(psUrls.getComputeResources_Url(ps_vpg.id))
						.onSuccess(function (body) {
							this.next(null); // prevent callback immediately after onSuccess event
							my.moveArray(my.jsonParse(body), ps_vpg.computeResources);
							my.log(ps_vpg.computeResources);
							ghapFrisby.create(authHeader, 'Get CE status')
								.get(psUrls.getVPG_status_Url(ps_vpg.userId))
								.onSuccess(function (body) {
									ps_vpg.status = body;
									console.log("\nCurrent CE status is '%s'",ps_vpg.status)
								})
								.next(callback)
								.toss()
						})
						.onError(function(response_status, body){
							if (response_status == 204 ) {
								console.log('No computing resources fond for the specified user.');
								ps_vpg.computeResources = [];
								return true;
							}
							return false;
						})
						.next(callback)
						.toss()
				})
				.next(callback)
				.toss();
		})
		.onError(function(response_status, body){
			if (response_status == 404 ) {
				console.log('Computing environment not fond for specified user.');
				return true;
			}
			return false;
		})
		.next(callback)
		.toss();
};

module.exports.terminateVPG = function(authHeader, ps_vpg, callback) {
	ghapFrisby.create(authHeader, 'Terminate CE')
		.delete(psUrls.getTerminateVPG_Url(ps_vpg.userId))
		.setLogMessage("Terminate CE for user with id '%s'", ps_vpg.userId)
		.next(callback)
		.toss();
};

// --------- Multi VPG ----------------------------------------------------------

/**
 * Get all of the stacks that currently exist.
 * @param {GhapAuthHeader} authHeader
 * @param {vpgStack[]} all_stacks - array  will be filled with stack objects
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetAllStacks = function(authHeader, all_stacks) {
	return ghapFrisby.create(authHeader, 'Get All MultiVpg Stacks')
		.get(psUrls.multiVPG_getAllStacks())
		.onSuccess(function (body) {
			var ghap_stacks = my.jsonParse(body);
			expect(ghap_stacks).toBeType(all_stacks);
			if (Array.isArray(ghap_stacks)){
				console.log(" %d stacks received.", ghap_stacks.length);
				all_stacks.length =0;
				ghap_stacks.forEach(function(ghap_stack){
					var vpg = psResources.makeVPG();
					my.copyProperties(ghap_stack, vpg);
					all_stacks.push(vpg);
				})
			}
		})
		.returnPromise();
};

/**
 * Gets all stacks provided for the user
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @param {vpgStack[]} vpg_array - RESULT
 * @param {Function} [callback]
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetStacks4User = function(authHeader, ums_user, vpg_array, callback) {
	return ghapFrisby.create(authHeader, 'Get Multi VPG')
		.get(psUrls.multiVPG_getStack4user(ums_user.getGuid()))
		.setLogMessage("Get MultiVPG  stack for user '%s' with id '%s'", ums_user.getName(), ums_user.getGuid())
		.onSuccess(function (body) {
			var ghap_vpg_array = my.jsonParse(body);
			expect(Array.isArray(ghap_vpg_array)).toBe(true);
			if (Array.isArray(ghap_vpg_array)){
				console.log(" User have %d VPG.", ghap_vpg_array.length);
				vpg_array.length =0;
				ghap_vpg_array.forEach(function(ghap_vpg){
					var vpg = psResources.makeVPG();
					my.copyProperties(ghap_vpg,vpg);
					vpg_array.push(vpg);
				})
			}
		})
		.next(callback)
		.returnPromise();
};

/**
 * Create a new Stack for the provided user and activity.
 * Only one stack per user is allowed. If one already exists response code 204 is returned otherwise 200
 * @param {GhapAuthHeader} authHeader
 * @param {ghapActivity} ghap_activity
 * @param {vpgStack} ps_vpg - RESULT
 * @param {Function} [callback]
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgCreateStack = function(authHeader, ghap_activity, ps_vpg, callback){
	return ghapFrisby.create(authHeader, 'Create CE')
		.put(psUrls.multiVPG_createVPG(ps_vpg.userId), ghap_activity, {json: true})
		.setLogMessage("Create CE '%s' for user with id '%s' (multiple VPG mode)", ghap_activity.activityName, ps_vpg.userId)
		.onSuccess(function (body) {
			expect(typeof body).toBe('object');
			if (typeof body === 'object') {
				my.copyProperties(body, ps_vpg);
			}
		})
		.next(callback)
		.returnPromise();
};

/**
 * Get the compute resources that are associated with a given stack
 * @param {GhapAuthHeader} authHeader
 * @param {vpgStack} ps_vpg - RESULTS pushed to ps_vpg.computeResources
 * @param {Function} [callback]
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetComputeResources4Stack = function(authHeader, ps_vpg, callback){
	return ghapFrisby.create(authHeader, 'Get Compute Resources')
		.get(psUrls.multiVPG_getComputeResources4Stack(ps_vpg.id))
		.setLogMessage("Get compute resources for VPG stack with id '%s' (multiple VPG mode)", ps_vpg.id)
		.onSuccess(function (body) {
			my.moveArray(my.jsonParse(body), ps_vpg.computeResources);
			//my.logJSON(ps_vpg.computeResources);
			console.log(" VPG have %d compute resources.", ps_vpg.computeResources.length);
		})
		.next(callback)
		.returnPromise();
};

/**
 * Get the compute resources that are associated with a given user
 * @param {GhapAuthHeader} authHeader
 * @param {UmsUserType} ums_user
 * @param {computeResource[]} comp_res_array - RESULTS
 * @param {Function} [callback]
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetComputeResources4User = function(authHeader, ums_user, comp_res_array, callback){
	return ghapFrisby.create(authHeader, 'Get Compute Resources For User')
		.get(psUrls.multiVPG_getComputeResources4user(ums_user.getGuid()))
		.setLogMessage("Get compute resources for user '%s' (multiple VPG mode)", ums_user.getName())
		.onSuccess(function (body) {
			my.moveArray(my.jsonParse(body), comp_res_array);
			//my.logJSON(body);
			console.log(" User have %d compute resources.", comp_res_array.length);
		})
		.next(callback)
		.returnPromise();
};

/**
 * Get the compute resources that are currently provisioned
 * @param {GhapAuthHeader} authHeader
 * @param {computeResource[]} comp_res_array - RESULTS
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetAllComputeResources = function(authHeader, comp_res_array){
	return ghapFrisby.create(authHeader, 'Get All Compute Resources')
		.get(psUrls.multiVPG_getAllComputeResources())
		.onSuccess(function (body) {
			my.moveArray(my.jsonParse(body), comp_res_array);
			//my.logJSON(body);
			console.log(" %d compute resources received.", comp_res_array.length);
		})
		.returnPromise();
};

/**
 * @typedef {object} awsUserInstance
 * @property {number} coreCount - 2
 * @property {string} address - ip '54.173.6.223',
 * @property {string} dnsname - ''
 * @property {string} instanceId - 'i-80351109'
 * @property {string} status - 'running'
 * @property {string} instanceOsType - 'Linux'
 * @property {string} vpgId - '95cd3b71-0952-4310-bcd0-7ef9fb1e3fc2' UNDEFINED for orphaned instances
 * @property {string} stackId - 'arn:aws:...ec2-50d5cd24fac6'
 * @property {string} userId - '24f9b8eb-d25d-4b09-8c79-4fee826feb0c'
 * @property {string} launchTime: '2016-02-04T12:54:08.000Z'
 * @property {string} imageId 'ami-7452711e'
 */

/**
 * Get the instances that are currently provisioned by AWS for GHAP users
 * @param authHeader
 * @param {awsUserInstance[]} aws_instances_array - RESULTS
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetAllAwsInstances = function(authHeader, aws_instances_array){
	return ghapFrisby.create(authHeader, 'Get All AWS Instances')
		.get(psUrls.multiVPG_getAllAwsInstances())
		.onSuccess(function (body) {
			my.moveArray(my.jsonParse(body), aws_instances_array);
			console.log(" %d instances received.", aws_instances_array.length);
		})
		.returnPromise();
};

/**
 * Get an RDP file for an Instance
 * @param {GhapAuthHeader} authHeader
 * @param {computeResource} compute_resource
 * @param {function} [callback]
 * @returns {Promise} resolved with empty value or rejected
 */
module.exports.multiVpgGetRdpFile = function(authHeader, compute_resource, callback){
	return ghapFrisby.create(authHeader, 'Get Rdp File')
		.get(psUrls.multiVPG_rdpFile(compute_resource.instanceId, compute_resource.instanceOsType, compute_resource.address))
		.setLogMessage("Get RDP file for '%s' instance with IP '%s'", compute_resource.instanceOsType, compute_resource.address)
		.expectHeader('Content-Type', 'application/rdp')
		.expectHeaderContains('Content-Disposition', 'filename='+compute_resource.instanceId)
		.expectBodyContains(compute_resource.address)
		.onSuccess(function (body) {
			console.log(' RDP-file content:');
			console.log(body);
		})
		.next(callback)
		.returnPromise();
};

module.exports.multiVpgGetStatuses4User = function(authHeader, ums_user, vpg_array) {
	var getMultiVPG_promise = exports.multiVpgGetStacks4User(authHeader, ums_user, vpg_array);
	return getMultiVPG_promise.then( function(){
		var calls = [];
		vpg_array.forEach(function(vpg){
			calls.push(	exports.multiVpgGetComputeResources4Stack(authHeader, vpg) )
		});
		return Q.all(calls)
	})
};

module.exports.multiVpgPause = function(authHeader, ghap_activity, ums_user, callback){
	return ghapFrisby.create(authHeader, 'Pause VPG')
		.put(psUrls.multiVPG_pause(ums_user.getGuid()), ghap_activity, {json: true})
		.setLogMessage("Pause CE for '%s' activity for user with id '%s' (multiple VPG mode)", ghap_activity.activityName, ums_user.getGuid())
		.next(callback)
		.returnPromise();
};

module.exports.multiVpgResume = function(authHeader, ghap_activity, ums_user, callback){
	return ghapFrisby.create(authHeader, 'Pause VPG')
		.put(psUrls.multiVPG_resume(ums_user.getGuid()), ghap_activity, {json: true})
		.setLogMessage("Resume CE for '%s' activity for user with id '%s' (multiple VPG mode)", ghap_activity.activityName, ums_user.getGuid())
		.next(callback)
		.returnPromise();
};


module.exports.multiVpgTerminate = function(authHeader, user_id, activity_id, callback) {
	return ghapFrisby.create(authHeader, 'Terminate CE')
		// add empty body and json request type to prevent 415 (Unsupported Media Type) error.
		.delete(psUrls.multiVPG_terminate(user_id, activity_id),{},{json: true})
		.setLogMessage("Terminate CE for user with ID '%s' for activity with ID '%s'", user_id, activity_id)
		.next(callback)
		.returnPromise();
};

module.exports.waitStatuses = function(authHeader, ums_user, awaiting_status, max_exec_time_ms, delay_between_attempts_ms){
	var deferred = Q.defer();

	if (typeof  max_exec_time_ms !== 'number') max_exec_time_ms = 3 * 60 * 1000;
	if (typeof delay_between_attempts_ms !== 'number') delay_between_attempts_ms = 30 * 1000;
	var userVPGs = [];
	var start = new Date();
	var exec_time_ms;

	function isAllStatuses(awaiting_status){

		if (userVPGs.length === 0){
			console.log("%s have no active VPGs", ums_user.getName());
			return false;
		}

		var res = true;

		userVPGs.forEach( function(vpg){
			if (vpg.computeResources.length){
				vpg.computeResources.forEach( function(comp_res){
					console.log("%s node have '%s' status.", comp_res.instanceOsType, comp_res.status);
					res = res && (comp_res.status == awaiting_status)
				});
			} else {
				res = false;
			}
		});

		return res;
	}

	function check() {
		exports.multiVpgGetStatuses4User(authHeader, ums_user, userVPGs)
			.then(function () {
				exec_time_ms = new Date() - start;
				if (isAllStatuses(awaiting_status)) {
					console.log("\nAll computing environments got '%s' status. Waiting time was %s.", awaiting_status, my.logTime(exec_time_ms));
					deferred.resolve();
				} else {
					console.log("\nWaiting time is %s", my.logTime(exec_time_ms));
					if (exec_time_ms < max_exec_time_ms) {
						my.pauseJasmine(delay_between_attempts_ms);
						check();
					} else {
						console.error("Awaiting time for '%s' statuses exceeded %d minutes", awaiting_status, max_exec_time_ms / 1000 / 60);
						deferred.reject(new Error('Awaiting VPG stack statuses timeout.'));
					}
				}
			})
	}

	check();

	return deferred.promise;
};

// --------- Personal Storage ---------------------------------------------------

module.exports.createPersonalStorage = function(authHeader, ps_storage, callback) {
	ghapFrisby.create(authHeader, 'Create Personal Storage')
		.put(psUrls.getCreateStorage_Url(ps_storage.userId, ps_storage.size))
		.setLogMessage("Create %dMB personal storage for user with ID '%s' ", ps_storage.size, ps_storage.userId)
		.next(callback)
		.returnPromise();
};

module.exports.getPersonalStorage = function(authHeader, ps_storage, callback) {
	ghapFrisby.create(authHeader, 'Check If Personal Storage Exists')
		.get(psUrls.getExistsStorage_Url(ps_storage.userId))
		.setLogMessage("Check If Personal Storage Exists for user with ID '%s' ", ps_storage.userId)
		.onSuccess(	function (body) {
			this.next(null); // prevent callback immediately after onSuccess event
			ghapFrisby.create(authHeader, 'Get Personal Storage Data')
				.get(psUrls.getGetStorage_Url(ps_storage.userId))
				.onSuccess(	function (body) {
					my.copyProperties(my.jsonParse(body), ps_storage);
				})
				.next(callback)
				.toss()
		})
		.onError(function(response_status, body){
			if ( (response_status == 404) || (response_status == 204) ) {
				console.log('Personal Storage not fond for the specified user.');
				ps_storage.id = null;
				return true;
			}
			return false;
		})
		.next(callback)
		.toss();
};

module.exports.deletePersonalStorage = function(authHeader, ps_storage, callback) {
	ghapFrisby.create(authHeader, 'Delete Personal Storage Data')
		.get(psUrls.getDeleteStorage_Url(ps_storage.userId))
		.setLogMessage("Delete personal storage for user with ID '%s' ", ps_storage.userId)
		.next(callback)
		.returnPromise();
};
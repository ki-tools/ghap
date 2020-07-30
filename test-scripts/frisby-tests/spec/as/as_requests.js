var my = require('./../Common/ghap-lib');

var ghapFrisby = require('./../Common/ghap-frisby');

var asUrls = require('./as_urls');
var asActivity = require('./as_activity');
/**
 * @typedef {object} ghapActivity
 * @property {string} id ('56cb9640-589a-4d66-baba-d3734cb1ed36')
 * @property {string} activityName ('Analysis via Linux Virtual Private Grid')
 * @property {number} minimumComputationalUnits
 * @property {number} maximumComputationalUnits
 * @property {number} defaultComputationalUnits
 * @property {string} templateUrl
 * @property {string} os ('linux')
 */

/**
 * Push all Ghap activities to allActivities_array
 * @param {GhapAuthHeader} authHeader
 * @param {ghapActivity[]} allActivities_array
 * @param {Function} [callback]
 * @returns {Promise}
 */
module.exports.getAllActivities = function(authHeader, allActivities_array, callback) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Activities', EXPECTED_STATUS)
		.get( asUrls.getAllActivities_Url() )
		.expectJSONTypes('*',asActivity.getActivity_jsonTypes())
		.onSuccess(function (body) {
			my.moveArray(my.jsonParse(body),allActivities_array);
			expect(allActivities_array.length).toBeGreaterThan(2);
			console.log(" %d activities recieved.",allActivities_array.length)
		})
		.next(callback)
		.returnPromise();
};

module.exports.createActivity = function(authHeader, as_activity, callback){
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader, 'Create Activity', EXPECTED_STATUS)
		.put(asUrls.getCreateActivity_Url(), as_activity.getCreateActivity_json(), {json: true})
		.setLogMessage("Create activity '%s'", as_activity.activityName)
		.onSuccess(function (body) {
			my.copyProperties(my.jsonParse(body), as_activity);
			expect(as_activity.activityName).toBeDefined();
			expect(as_activity.id).toBeDefined();
			console.log("\nActivity '%s' is created with id '%s'", as_activity.activityName, as_activity.id);
		})
		.next(callback)
		.toss();
};

module.exports.deleteActivity = function(authHeader, as_activity, callback){
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader, 'Delete Activity By ID', EXPECTED_STATUS)
		.delete( asUrls.getDeleteActivityById_Url(as_activity.id) )
		.setLogMessage("Delete activity '%s' by ID '%s'", as_activity.activityName, as_activity.id)
		.next(callback)
		.toss();
};

module.exports.createActivityRoleAssociation = function(authHeader, as_activity, ums_role, callback){
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader,'Associate Activity With Role', EXPECTED_STATUS)
		.put( asUrls.getAssociateActivityWithRole_Url(as_activity.activityName, ums_role.guid	) )
		.setLogMessage("Associate Activity '%s' with role '%s'", as_activity.activityName, ums_role.name)
		.next(callback)
		.toss();
};

module.exports.getARAssociationsForRole = function(authHeader, ums_role, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get AR Associations', EXPECTED_STATUS)
		.get( asUrls.getARAssociationsForRole_Url(ums_role.guid) )
		.setLogMessage("Get ActivityRole-Associations for '%s'", ums_role.name)
		.onSuccess(function(body){
			my.moveArray(my.jsonParse(body),ums_role.ar_associations);
			console.log("\n%s",my.getInspectObjStr(ums_role.ar_associations));
			console.log("\n'%s' have %d activities.", ums_role.name, ums_role.ar_associations.length);
		})
		.next(callback)
		.returnPromise();
};

module.exports.getActivityById = function(authHeader, activity_id, as_activity, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Activity By Id', EXPECTED_STATUS)
		.get( asUrls.getActivityById_Url(activity_id) )
		.setLogMessage("Get Activity By ID '%s'", activity_id)
		.expectJSONTypes(asActivity.getActivity_jsonTypes())
		.onSuccess(function(body){
			expect(typeof body).toBe('string');
			if (typeof body === 'string') {
				my.copyProperties(my.jsonParse(body), as_activity);
				console.log("\n'%s' activity found.",as_activity.activityName);
			}
		})
		.next(callback)
		.returnPromise();
};

module.exports.getActivityByName = function(authHeader, activity_name, as_activity, callback){
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader, 'Get Activity By Name', EXPECTED_STATUS)
		.get( asUrls.getActivityByName_Url(activity_name) )
		.setLogMessage("Get Activity By Name for '%s'", activity_name)
		.expectJSONTypes(asActivity.getActivity_jsonTypes())
		.onSuccess(function(body){
			expect(typeof body).toBe('string');
			if (typeof body === 'string') {
				my.copyProperties(my.jsonParse(body), as_activity);
				console.log("\n%s",my.getInspectObjStr(as_activity));
			}
		})
		.next(callback)
		.toss();
};

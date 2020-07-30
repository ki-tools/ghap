var my = require('./../ums/ums_common');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});

var asUrls = require('./as_urls');

module.exports.getAllActivities = function(authHeader, allActivities_array, callback) {
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Get All Activities')
		.get( asUrls.getAllActivities_Url() )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSONTypes('*',{
			id: String,
			activityName: String,
			minimumComputationalUnits: Number,
			maximumComputationalUnits: Number,
			defaultComputationalUnits: Number,
			templateUrl: String
		})
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get all activities - check response status code and content.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				// http://stackoverflow.com/questions/1232040/how-to-empty-an-array-in-javascript
				allActivities_array.length = 0;
				var parsed_array = my.jsonParse(body);
				if (parsed_array instanceof Array) {
					var element = parsed_array.shift();
					while(element) {
						allActivities_array.push( element  );
						element = parsed_array.shift();
					}
				}

				expect(allActivities_array.length).toBeGreaterThan(2);
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.createActivity = function(authHeader, as_activity, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Create Activity')
		.put(asUrls.getCreateActivity_Url(),
		  as_activity.getCreateActivity_json(), {json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create Activity - check response status code.');
			my.logExecutionTime(start);
			//console.log(body);

			if (this.current.response.status == 200) {
				expect(typeof body).toBe('object');
				if (typeof body === 'object') {
					var created_activity = my.jsonParse(body);
					expect(created_activity.id).toBeDefined();
					as_activity.id = created_activity.id;
					console.log('Activity '+as_activity.activityName+' is created with id '+as_activity.id);
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.deleteActivity = function(authHeader, as_activity, callback){

	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Delete Activity')
		//.delete( asUrls.getDeleteActivity_Url(),
		//{	"activityName":"AnActivity2"},
		//{json: true}
		//)
		.delete( asUrls.getDeleteActivityById_Url(as_activity.id) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete Activity by ID - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.createActivityRoleAssociation = function(authHeader, as_activity, ums_role, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Associate Activity With Role')
		.put( asUrls.getAssociateActivityWithRole_Url(as_activity.activityName, ums_role.guid	) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Associate Activity With Role - check response status code.');
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};

module.exports.getARAssociationsForRole = function(authHeader, ums_role, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Get Associations For Role')
		.get( asUrls.getARAssociationsForRole_Url(ums_role.guid) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get AR-Associations For Role '+ums_role.name+' - check response status code.');
			console.log(body);
			my.logExecutionTime(start);

			var status_code = this.current.response.status;
			if (status_code == 200) {
				expect(typeof body).toBe('string');
				if (typeof body === 'string') {
					ums_role.ar_associations.length = 0;
					var parsed_body = my.jsonParse(body);
					if (parsed_body instanceof Array) {
						var element = parsed_body.shift();
						while (element) {
							ums_role.ar_associations.push(element);
							element = parsed_body.shift();
						}
					}
					console.log(ums_role.ar_associations);
				}
			} else
				console.log ('Unexpected status code '+status_code+'. Body:'+my.endOfLine+body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.getActivityById = function(authHeader, activity_id, as_activity, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Get Activity By Id')
		.get( asUrls.getActivityById_Url(activity_id) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSONTypes(as_activity.getActivity_jsonTypes())
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get Activity By Id - check response status code and content.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				expect(typeof body).toBe('string');
				if (typeof body === 'string') {
					var retrieved_activity = my.jsonParse(body);
					my.copyProperties(retrieved_activity, as_activity);
					//console.log(as_activity);
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.getActivityByName = function(authHeader, activity_name, as_activity, callback){
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Get Activity By Id')
		.get( asUrls.getActivityByName_Url(activity_name) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSONTypes(as_activity.getActivity_jsonTypes())
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get Activity By Name - check response status code and content.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				expect(typeof body).toBe('string');
				if (typeof body === 'string') {
					var retrieved_activity = my.jsonParse(body);
					my.copyProperties(retrieved_activity, as_activity);
					//console.log(as_activity);
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

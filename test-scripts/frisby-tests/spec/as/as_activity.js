// Activity Service object
// constructor
function asActivity(activity_name) {
	this.id = '';
	this.activityName = activity_name;
	this.minimumComputationalUnits = 1;
	this.maximumComputationalUnits = 10;
	this.defaultComputationalUnits = 1;
	this.templateUrl = '';
	this.os = '';

	return this;
}

asActivity.prototype.getCreateActivity_json = function(){
	return {
		activityName: this.activityName,
		minimumComputationalUnits: this.minimumComputationalUnits,
		maximumComputationalUnits: this.maximumComputationalUnits,
		defaultComputationalUnits: this.defaultComputationalUnits,
		templateUrl: this.templateUrl
	}
};

module.exports.makeActivity = function(activity_name) {
	return new asActivity(activity_name);
};

module.exports.getActivity_jsonTypes = function(){
	return {
		id: String,
		activityName: String,
		minimumComputationalUnits: Number,
		maximumComputationalUnits: Number,
		defaultComputationalUnits: Number,
		templateUrl: String
	}
};
function personalStorage (ums_user, gb_size_num) {
	this.guid = '';
	this.uuid = ums_user.getGuid();
	this.g_size = gb_size_num;

	return this
};

personalStorage.prototype.getCreateStorage_json = function() {
	return {
		uuid: this.uuid,
		size: this.g_size
	}
};

module.exports.makeStorage = function (ums_user, size) {
	return new personalStorage(ums_user, size);
};

//--------------------------------------------------------

function virtualPrivateGrid (ums_user) {
	this.id = '';
	this.activityId = '';
	this.userId = ums_user.getGuid();
	this.stackId ='';
	this.pemKey = '';

	return this
};

virtualPrivateGrid.prototype.getCreateVPG_json = function() {
	return  {
		id: 'd4e332ce-4fb9-4edd-8625-481a87af01e4',
		activityName: 'any',
		//minimumComputationalUnits: 2,
		//maximumComputationalUnits: 10,
		//defaultComputationalUnits: 1,
		templateUrl: 'https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-dev/analysis-vpg-activity.json' }
};

module.exports.makeVPG = function (ums_user) {
	return new virtualPrivateGrid(ums_user);
};
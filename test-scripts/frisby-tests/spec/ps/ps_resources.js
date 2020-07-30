function PersonalStorage (ums_user, storage_size_in_mb) {
	this.id = '';
	this.userId = null;
	if (ums_user) {
		this.userId = ums_user.getGuid();
	}
	this.size = 0;
	if (storage_size_in_mb) {
		this.size = storage_size_in_mb;
	}
	this.volumeId = '';
	this.availabilityZone = '';

	return this
}

module.exports.makeStorage = function (ums_user, size) {
	return new PersonalStorage(ums_user, size);
};

//--------------------------------------------------------

/**
 * @typedef {object} vpgStack
 * @property {string} id - vpg_id
 * @property {string} activityId
 * @property {string} userId - user guid
 * @property {string} stackId - AWS stack id
 * @property {string} pemKey
 * @property {number|null} autoScaleMinInstanceCount - only for LinuxVPG stack;
 * @property {number|null} autoScaleMaxInstanceCount - only for LinuxVPG stack;
 * @property {number|null} autoScaleDesiredInstanceCount - only for LinuxVPG stack;
 * @property {string} status - missed in GhapStack;
 * @property {computeResource[]} computeResources - missed in GhapStack;
 */

/**
 * @typedef {object} computeResource
 * @property {number} coreCount      - 4
 * @property {string} address        - '52.71.251.144'
 * @property {string} instanceId     - 'i-3bed1b98'
 * @property {string} status         - 'running'
 * @property {string} instanceOsType - 'Windows'
 * @property {string} vpgId          - '709c8d21-3a88-4889-9cdf-d828a37c77d6'
 * @property {string} stackId        - 'arn:aws:cloudformation:...-5001b491380a'
 * @property {string} userId         - 'c52787d2-f6b5-4c89-ba09-23f0b7f5e45f'
 * @property {string} launchTime     - '2016-02-04T15:50:00.000Z'
 * @property {string} imageId        - 'ami-7452711e' },
 */

/**
 * 
 * @param ums_user
 * @returns {vpgStack}
 * @constructor
 */
function VirtualPrivateGrid (ums_user) {
	this.id = '';
	this.activityId = '';
	this.userId = '';

	if (typeof ums_user === 'string')
		this.userId = ums_user;
	else if (typeof ums_user === 'object' && ums_user)  // except ums_user === null
		this.userId = ums_user.getGuid();

	this.stackId ='';
	this.pemKey = '';

	this.status = '';
	this.computeResources = [];

	this.autoScaleMinInstanceCount = null;
	this.autoScaleMaxInstanceCount = null;
	this.autoScaleDesiredInstanceCount = null;

	return this
}

/**
 * Create mew VPG instance
 * @param {ums_user} ums_user
 * @returns {vpgStack}
 */
module.exports.makeVPG = function (ums_user) {
	return new VirtualPrivateGrid(ums_user);
};
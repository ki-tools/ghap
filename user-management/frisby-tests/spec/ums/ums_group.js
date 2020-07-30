// UMS group object
// constructor
function UmsGroup(parent_dn_str, group_name_str, description_str) {
	this.parent_dn = parent_dn_str;
	this.name = group_name_str;
	if(typeof description_str !== 'string')
		description_str = '';
	this.description = description_str;
	this.guid = '';

	this.members = [];
	this.expected_members = [];

	return this;
}

UmsGroup.prototype.getDn = function (firstAttributeName_str) {
	if (typeof firstAttributeName_str === 'undefined') firstAttributeName_str = 'cn';
	return firstAttributeName_str + '=' + this.name + ',' + this.parent_dn;
};

UmsGroup.prototype.getCreateGroup_json = function() {
	return {
		parentDn: this.parent_dn,
		name: this.name,
		description: this.description
	}
};

UmsGroup.prototype.getGroup_json = function(firstAttributeName_str) {
	return {
		dn: this.getDn(firstAttributeName_str),
		objectClass: 'group',
		name: this.name,
		description: this.description
	}
};

UmsGroup.prototype.addExpectedMember = function(ums_user) {
	this.expected_members.push(ums_user);
};

UmsGroup.prototype.deleteExpectedMember = function(ums_user) {
	var memberIndex = -1;
	for(var i=0; i< this.expected_members.length; i++)
		if (ums_user.getDn() === this.expected_members[i].getDn()){
			memberIndex = i;
			break;
		}
	if (memberIndex > 0) {
		this.expected_members.splice(memberIndex,1);
		return true;
	}
	return false;
};

UmsGroup.prototype.checkMembers = function(callback) {
	console.log('Checking expected members against existing members in '+this.name);
	var self = this;
	expect(this.members.length).toEqual(this.expected_members.length);
	if (this.members.length === this.expected_members.length)
		this.expected_members.forEach( function(expected_member) {
			var found =	self.members.some( function(member){
				return (member.dn === expected_member.getDn());
			} );
			expect(found).toBe(true);
		} );

	if (typeof callback === 'function')
		callback( jasmine.getEnv().currentSpec.results().failedCount );
};

module.exports.create = function(parent_dn_str, group_name_str, description_str) {
	return new UmsGroup(parent_dn_str, group_name_str, description_str);
};

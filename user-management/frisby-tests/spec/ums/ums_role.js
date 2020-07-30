// UMS role object
// constructor
function UmsRole(role_name_str, description_str) {
	this.parent_dn = "CN=Roles,DC=prod,DC=ghap,DC=io";
	this.name = role_name_str;
	if(typeof description_str !== 'string')
		description_str = '';
	this.description = description_str;
	this.guid = '';

	this.ar_associations = [];

	return this;
}

UmsRole.prototype.getDn = function (firstAttributeName_str) {
	if (typeof firstAttributeName_str === 'undefined') firstAttributeName_str = 'cn';
	return firstAttributeName_str + '=' + this.name + ',' + this.parent_dn;
};

UmsRole.prototype.getCreateRole_json = function() {
	return {
		parentDn: this.parent_dn,
		name: this.name,
		description: this.description
	}
};

UmsRole.prototype.getRole_json = function(firstAttributeName_str) {
	return {
		dn: this.getDn(firstAttributeName_str),
		objectClass: 'group',
		guid: this.guid,
		name: this.name,
		description: this.description
	}
};

module.exports.makeRole = function(role_name_str, description_str) {
	return new UmsRole(role_name_str, description_str);
};

// UMS role object
// constructor
var my = require('./../Common/ghap-lib');

function UmsRole(role_name_str, description_str) {
	if(typeof role_name_str !== 'string')	role_name_str = '';
	if(typeof description_str !== 'string')	description_str = '';

	this.parent_dn = "CN=Roles,DC=prod,DC=ghap,DC=io";
	this.name = role_name_str;
	this.description = description_str;
	this.guid = '';
	this.objectClass = 'group';
	this.dn = '';

	this.ar_associations = [];

	return this;
}

UmsRole.prototype.getDn = function () {
	if (this.dn === '')
		return 'CN=' + this.name + ',' + this.parent_dn;
	else
		return this.dn;
};

UmsRole.prototype.getCreateRole_json = function() {
	return {
		parentDn: this.parent_dn,
		name: this.name,
		description: this.description
	}
};

UmsRole.prototype.getRole_ExpectedJson = function() {
	var dn = this.getDn();
	return {
		dn: function(expected_dn) {return expected_dn.toLowerCase() === dn.toLocaleLowerCase()},
		objectClass: 'group',
		guid: this.guid,
		name: this.name,
		description: this.description
	}
};

module.exports.makeRole = function(role_name_str, description_str) {
	return new UmsRole(role_name_str, description_str);
};

module.exports.findRoleByName = function(allRoles_array, role_name){
	if (!(allRoles_array instanceof Array)) {
		console.error('\nInvalid type of allRoles_array parameter.');
		return null;
	}

	if (allRoles_array.length === 0) {
		console.error('\allRoles_array is empty.');
		return null;
	}

	var filtered_roles = allRoles_array.filter(function(role){return role.name === role_name});

	if (filtered_roles.length === 1){
		var ums_role = exports.makeRole();
		my.copyProperties(filtered_roles[0],ums_role);
		return ums_role;
	}

	if (filtered_roles.length === 0)
		console.error("\nNo '%s' role found in all roles array.", role_name);
	else
		console.error("\nMultiple definitions for role '%s' in all roles array.", role_name);
	return null;
};
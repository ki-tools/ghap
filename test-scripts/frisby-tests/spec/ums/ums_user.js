var my = require('./../Common/ghap-lib');

/**
 * @typedef {object} UmsUserType
 * @property {Function} getName
 * @property {Function} setName
 * @property {Function} getFirstName
 * @property {Function} setFirstName
 * @property {Function} getLastName
 * @property {Function} setLastName
 * @property {Function} getEmail
 * @property {Function} setEmail
 * @property {Function} getPassword
 * @property {Function} setPassword
 * @property {Function} getObjectClass
 * @property {Function} setObjectClass
 * @property {Function} getDisabled
 * @property {Function} setDisabled
 * @property {Function} addRole
 * @property {Function} getRoles
 * @property {Function} setRoles
 * @property {Function} getGuid
 * @property {Function} setGuid
 * @property {Function} getFullName
 * @property {Array} projects
 * @property {GhapAuthHeader} authHeader
 * @property {Function} setAuthHeader
 * @property {Function} getDn
 * @property {Function} getParentDn
 */

/**
 * Constructor of UmsUser
 * @param first_name
 * @param last_name
 * @param email_str
 * @param password_str
 * @param name_str
 * @returns {UmsUserType}
 * @constructor
 */
function UmsUser (first_name, last_name, email_str, password_str, name_str) {
	var
		parentDn = "CN=Users,DC=prod,DC=ghap,DC=io",
		name,	firstName, lastName, email,	password,
		objectClass = 'user',
		disabled = false,
		roles = [],
		guid = 'unknown';

	if (last_name == null){
		name = first_name.toLowerCase();
		firstName = first_name;
		lastName = 'user';
	} else {
		if (name_str)
			name = name_str.toLowerCase();
		else {
			name = first_name.toLowerCase() ;
			if (last_name) name += '.' + last_name.toLowerCase();
		}
		firstName = first_name;
		lastName = last_name;
	}

	if (!email_str) email_str = 'success@simulator.amazonses.com';
	email = email_str;

	if (!password_str) password_str = '#4$asDFG';
	password = password_str;

	this.getParentDn = function() {return parentDn};
	this.getName = function() {return name};
	this.setName = function(name_str) {	name = name_str;	};
	this.getFirstName = function() {return firstName};
	this.setFirstName = function(name_str) {	firstName = name_str;	};
	this.getLastName = function() {return lastName};
	this.setLastName = function(name_str) {	lastName = name_str;	};
	this.getEmail = function() {return email};
	this.setEmail = function(email_str) {	email = email_str;	};
	this.getPassword = function() {return password};
	this.setPassword = function(new_password_str) {password = new_password_str};
	this.getObjectClass = function() {return objectClass};
	this.setObjectClass = function(objectClass_str) {objectClass = objectClass_str};
	this.getDisabled = function() {return disabled};
	this.setDisabled = function(bool_disabled) {disabled = bool_disabled};
	this.addRole = function(ums_role) {roles.push(ums_role)};
	this.getRoles = function() {return roles};
	this.setRoles = function(roles_array) {my.moveArray(roles_array, roles)};
	this.getGuid = function() {return guid};
	this.setGuid = function(guid_str) { if(guid_str) guid = guid_str; else guid = 'unknown'};

	this.getFullName = function() {
		var full_name = firstName;
		if (lastName) full_name += ' ' + lastName;
		return full_name;
	};

	this.projects = [];

    this.authHeader = {
        Name: '',
        Value: ''
    };
    this.setAuthHeader = function(auth_header) {
        this.authHeader.Name = auth_header.Name;
        this.authHeader.Value = auth_header.Value;
    };

    return this;
}

UmsUser.prototype.getDn = function () {
	return 'CN=' + this.getFullName() + ',' + this.getParentDn();
};

UmsUser.prototype.getCreate_json = function () {
	return {
		parentDn: this.getParentDn(),
		name: this.getName(),
		firstName: this.getFirstName(),
		lastName: this.getLastName(),
		email: this.getEmail(),
		password: this.getPassword()
		// passwordConfirmation: password
	}
};

UmsUser.prototype.getUser_ExpectedJson = function () {
	var dn = this.getDn();
	return {
		dn: function(expected_dn) {return expected_dn.toLowerCase() === dn.toLocaleLowerCase()},
		name: this.getName(),
		firstName: this.getFirstName(),
		lastName: this.getLastName(),
		fullName: this.getFullName(),
		email: this.getEmail(),
		objectClass: this.getObjectClass(),
		disabled: this.getDisabled(),
		guid: this.getGuid()
	}
};

UmsUser.prototype.updateUserData = function (new_values_json) {
	for ( var key_name in new_values_json ) {
		switch (key_name) {
			case 'email':
				this.setEmail(new_values_json[key_name]);
				break;

			case 'firstName':
				this.setFirstName(new_values_json[key_name]);
				break;

			case 'lastName':
				this.setLastName(new_values_json[key_name]);
				break;

			case 'name':
				this.setName(new_values_json[key_name]);
				break;

			case 'quid':
				this.setGuid(new_values_json[key_name]);
				break;

			default:
				console.error("Unknown key '%s' in new_values_json", key_name);
				return false;
		}
	}
	return true;
};

/**
 *
 * @param {string} first_name
 * @param {string} [last_name]
 * @param {string} [email_str]
 * @param {string} [password_str]
 * @param {string} [name_str]
 * @returns {UmsUserType}
 */
exports.makeUser = function(first_name, last_name, email_str, password_str, name_str){
	if (!first_name) first_name = "Guest";
	return new UmsUser(first_name, last_name, email_str, password_str, name_str);
};

/**
 *
 * @param {object} ghap_user
 * @param {object} [ums_user] optional
 * @returns {object} - new or updated ums_user
 */
exports.makeUserFromGhapUser = function (ghap_user, ums_user){
	if (typeof ums_user !== 'object')
		ums_user = new UmsUser(ghap_user.name);

	ums_user.setName(ghap_user.name);
	ums_user.setFirstName(ghap_user.firstName);
	ums_user.setLastName(ghap_user.lastName);
	ums_user.setEmail(ghap_user.email);
	ums_user.setGuid(ghap_user.guid);
	ums_user.setDisabled(ghap_user.disabled);
	return ums_user;
};
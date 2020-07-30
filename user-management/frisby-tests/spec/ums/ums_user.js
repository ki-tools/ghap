function UmsUser (first_name, last_name, email_str, password_str) {
	var
		parentDn = "CN=Users,DC=prod,DC=ghap,DC=io",
		name,	firstName, lastName, email,	password,
		objectClass = 'user',
		disabled = false,
		roles = [],
		guid = '';

	if (last_name == null){
		name = first_name;
		firstName = first_name+'FirstName';
		lastName = first_name+'LastName';
		email = name+'@ghap.io';
		password = '#4$asDF';
	} else {
		name =first_name ;
		if (last_name) name += '.' + last_name;
		firstName = first_name;
		lastName = last_name;
		email = email_str;
		password = password_str
	}

	this.getParentDn = function() {return parentDn};
	this.getName = function() {return name};
	this.getFirstName = function() {return firstName};
	this.setFirstName = function(name_str) {	firstName = name_str;	};
	this.getLastName = function() {return lastName};
	this.setLastName = function(name_str) {	lastName = name_str;	};
	this.getEmail = function() {return email};
	this.setEmail = function(email_str) {	email = email_str;	};
	this.getPassword = function() {return password};
	this.getObjectClass = function() {return objectClass};
	this.getDisabled = function() {return disabled};
	this.addRole = function(ums_role) {roles.push(ums_role)};
	this.getRoles = function() {return roles};
	this.getGuid = function() {return guid};
	this.setGuid = function(guid_str) {guid = guid_str};

	this.getFullName = function() {
		var full_name = firstName;
		if (lastName) full_name += ' ' + lastName;
		return full_name;
	};

	return this;
}

UmsUser.prototype.getDn = function (firstAttributeName_str) {
	if (typeof firstAttributeName_str === 'undefined') firstAttributeName_str = 'CN';
	return firstAttributeName_str + '=' + this.getFullName() + ',' + this.getParentDn();
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

UmsUser.prototype.getUser_json = function (firstAttributeName_str) {
	return {
		dn: this.getDn(firstAttributeName_str),
		name: this.getName(),
		//firstName: this.getFirstName(),
		//lastName: this.getLastName(),
		fullName: this.getFullName(),
		//email: this.getEmail(),
		objectClass: this.getObjectClass(),
		disabled: this.getDisabled()
	}
};

module.exports.makeUser = function(first_name, last_name, email_str, password_str){
	return new UmsUser(first_name, last_name, email_str, password_str);
};
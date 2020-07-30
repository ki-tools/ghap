function GhapProject (name_str, key_str, description_str) {
	this.id = null;
	this.name = name_str;
	this.key = key_str;
	this.description = description_str;

	this.grants = [];

	return this
}

GhapProject.prototype.getCreateProject_json = function(){
	return {
		'name': this.name,
		'key': this.key,
		'description': this.description
	}
};

GhapProject.prototype.addGrant = function(ghap_grant){
	this.grants.push(ghap_grant);
};

GhapProject.prototype.deleteGrant = function(ghap_grant){
	var grant_index = -1;
	for(var i=0; i< this.grants.length; i++)
		if (ghap_grant.id === this.grants[i].id){
			grant_index = i;
			break;
		}
	if (grant_index > 0) {
		this.grants.splice(grant_index,1);
		return true;
	}
	return false;
};

function GhapGrant (name_str) {
	this.id = null;
	this.name = name_str;
}

GhapGrant.prototype.getCreateGrant_json = function(){
	return {
		'name': this.name
	}
};

module.exports.makeProject = function (name_str, key_str, description_str) {
	return new GhapProject(name_str, key_str, description_str);
};

module.exports.makeGrant = function (name_str) {
	return new GhapGrant(name_str);
};

module.exports.findProjectByKey = function(all_projects, project_key){
	var filtered_projects = all_projects.filter(function (prj_res) {
		return (prj_res.key == project_key)
	});
	if (filtered_projects.length === 1)
		return filtered_projects[0];
	return null;
};

module.exports.findGrantByName = function(all_grants, grant_name){
	var filtered_grants = all_grants.filter(function (grant_res) {
		return (grant_res.name == grant_name)
	});
	if (filtered_grants.length === 1)
		return filtered_grants[0];
	return null;
};

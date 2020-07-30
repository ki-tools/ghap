/**
 * Created by Vlad on 06.11.2015.
 */

var assert = require('assert');

var my = require('./../../Common/ghap-lib');
var lib = require('./../compare-stash-prj-lib');

describe('listValues', function(){
    var array_of_objects = [];
    array_of_objects.push({"key1" : "val1", "key2" : "val2"});
    array_of_objects.push({"key1" : "val3", "key2" : "val4", "key3" : 'val5'});

    it('should list val1, val3 for key1', function() {
        var list = lib.listValues(array_of_objects, 'key1');
        assert.equal(list,"'val1', 'val3'");
    });

    it('should list val2, val4 for key2', function() {
        var list = lib.listValues(array_of_objects, 'key2');
        assert.equal(list,"'val2', 'val4'");
    });

    it('should list ???, val5 for key3', function() {
        var list = lib.listValues(array_of_objects, 'key3');
        assert.equal(list,"[????], 'val5'");
    })

});

var stashId = 1;
function StashProject(key_str) {
    this.id = '' + stashId++;
    this.key = key_str.toUpperCase();
    this.name = key_str + ' name';
    this.description = null;
    this.isPublic = true;
    this.type = 'NORMAL';
    this.permissions = null;
}

var grantId = 1;
function StashGrant(name_str) {
    this.id = '' + grantId++;
    this.name = name_str;
    this.slug = name_str.toLowerCase();
    this.scmId = 'git';
    this.state = 'AVAILABLE';
    this.statusMessage = 'Available';
    this.forkable = false;
    this.cloneUrl = 'https://username@git.ghap.io/stash/scm/prj/repo.git';
    this.permissions = null;
}

function createStashProjects(){
    var projects = [];
    var p;

    p = new  StashProject('PA');
    p.grants = [];
    p.grants.push(new StashGrant('A1'));
    p.grants.push(new StashGrant('A2'));
    p.grants.push(new StashGrant('A3'));
    projects.push(p);

    p = new  StashProject('PStash');
    p.grants = [];
    p.grants.push(new StashGrant('PS0'));
    p.grants.push(new StashGrant('PS1'));
    projects.push(p);

    p = new  StashProject('PB');
    p.grants = [];
    p.grants.push(new StashGrant('B1'));
    p.grants.push(new StashGrant('BY'));
    p.grants.push(new StashGrant('B2'));
    p.grants.push(new StashGrant('BX'));
    p.grants.push(new StashGrant('B3'));
    projects.push(p);

    return projects;
}

var prjResources = require('../../prj-prov/prj-prov_resources');

function createGhapProjects(){
    var projects = [];
    var p;

    p = prjResources.makeProject('PA', 'PA', null);
    p.addGrant( prjResources.makeGrant('A1') );
    p.addGrant( prjResources.makeGrant('A2') );
    p.addGrant( prjResources.makeGrant('AX') );
    p.addGrant( prjResources.makeGrant('A3') );
    projects.push(p);

    p = prjResources.makeProject('PB', 'PB', null);
    p.addGrant( prjResources.makeGrant('B1') );
    p.addGrant( prjResources.makeGrant('B2') );
    p.addGrant( prjResources.makeGrant('B3') );
    projects.push(p);

    p = prjResources.makeProject('PGhap', 'PGHAP', null);
    p.addGrant( prjResources.makeGrant('T1') );
    p.addGrant( prjResources.makeGrant('T2') );
    projects.push(p);

    return projects;
}

var stashProjects = createStashProjects();
var ghapProjects = createGhapProjects();

describe('getMissedProjects', function(){

    it('project PSTASH should missed in ghapProjects', function() {
        var pStash = my.findElementInArray(stashProjects, 'key', 'PSTASH');
        assert.notEqual(pStash, null);
        var src_length = stashProjects.length;
        var missed = lib.getMissedProjects(stashProjects, ghapProjects);
        assert.equal(missed.length, 1);
        assert.equal(missed[0], pStash);
        assert.equal(stashProjects.length, src_length-1);
    });

    it('project PGHAP should missed in stashProjects', function() {
        var pGhap = my.findElementInArray(ghapProjects, 'key', 'PGHAP');
        assert.notEqual(pGhap, null);
        var src_length = ghapProjects.length;
        var missed = lib.getMissedProjects(ghapProjects, stashProjects);
        assert.equal(missed.length, 1);
        assert.equal(missed[0], pGhap);
        assert.equal(stashProjects.length, src_length-1);
    });

    it('projects arrays should have identical keys', function() {
        var sp_len = stashProjects.length;
        var gp_len = ghapProjects.length;
        assert.equal(sp_len, gp_len);
        for (var i=0; i < sp_len; i++) {
            var sp = stashProjects[i];
            var gp = my.findElementInArray(ghapProjects, 'key', sp.key);
            assert.notEqual(gp, null);
        }
    })

});

describe('gerMissedGrants', function(){

    it('PB/BX,BY grants should be missed in ghapProjects', function () {
        var missed = lib.getMissedGrants( stashProjects, ghapProjects);
        assert.equal(missed.length, 1);
        assert.equal(missed[0].prjKey, 'PB');
        assert.equal(missed.length, 1);
        assert.equal(missed[0].grants.length, 2);
        var list_str = lib.listValues(missed[0].grants, 'name');
        assert.equal(list_str, "'BY', 'BX'");
    });

    it('PA/AX grant should be missed in stashProjects', function() {
        var pa = my.findElementInArray(ghapProjects, 'key', 'PA');
        var ax_grant = my.findElementInArray(pa.grants, 'name', 'AX');
        var missed = lib.getMissedGrants( ghapProjects, stashProjects);
        assert.equal(missed.length, 1);
        assert.equal(missed[0].grants.length, 1);
        assert.equal(missed[0].grants[0], ax_grant);
    });
});

function StashProjectPermissions(project_key, permission_str){
    this.prj_key = project_key;
    if (permission_str === 'RO') {
        this.prj_permission = 'PROJECT_READ'
    } else if (permission_str === 'RW') {
        this.prj_permission = 'PROJECT_WRITE';
    } else {
        this.prj_permission = 'missed';
    }
}

function createStashProjectsPermissions() {
    var res = [];
    res.push( new StashProjectPermissions('P1','RW'));
    res.push( new StashProjectPermissions('PX','RW'));
    res.push( new StashProjectPermissions('P2','RO'));
    res.push( new StashProjectPermissions('P3','RW'));
    res.push( new StashProjectPermissions('P4','RO'));
    res.push( new StashProjectPermissions('P5',''));
    return res;
}

function GhapProjectPermissions(project_key, permission_str){
    this.id = 1;
    this.name = 'Test Project';
    this.key = project_key;
    if (permission_str) {
        this.permissions = ['READ'];
        if (permission_str === 'RW')
            this.permissions.push('WRITE');
    } else {
        this.permissions = [];
    }
}

function createGhapProjectsPermissions() {
    var res = [];
    res.push( new GhapProjectPermissions('P1','RW'));
    res.push( new GhapProjectPermissions('PY','RW'));
    res.push( new GhapProjectPermissions('P2','RW'));
    res.push( new GhapProjectPermissions('P3','RO'));
    res.push( new GhapProjectPermissions('P4','RO'));
    res.push( new GhapProjectPermissions('P5',''));
    return res;
}

describe('getProjectsPermissionsDiffs', function(){
   var diffs = lib.getProjectsPermissionsDiffs(
       createStashProjectsPermissions(),
       createGhapProjectsPermissions()
   );

    it('should be 4 differences', function(){
        console.log(diffs);
        assert.equal(diffs.length, 4);
    });

    it('PX should be RW in STASH but missed in GHAP grants', function(){
        assert.equal(diffs[0].prj_key, 'PX');
        assert.equal(diffs[0].permissions_diff_str, 'STASH: PROJECT_WRITE; GHAP: GHAP_PROJECT_NOT_FOUND');
    });

    it('P2 should be RO in STASH but RW in GHAP grants', function(){
        assert.equal(diffs[1].prj_key, 'P2');
        assert.equal(diffs[1].permissions_diff_str, 'STASH: PROJECT_READ; GHAP: READ,WRITE');
    });

    it('P2 should be RW in STASH but RO in GHAP grants', function(){
        assert.equal(diffs[2].prj_key, 'P3');
        assert.equal(diffs[2].permissions_diff_str, 'STASH: PROJECT_WRITE; GHAP: READ');
    });

    it('PY should be missed in STASH but RW in GHAP grants', function(){
        assert.equal(diffs[3].prj_key, 'PY');
        assert.equal(diffs[3].permissions_diff_str, 'STASH: STASH_PROJECT_NOT_FOUND; GHAP: READ,WRITE');
    })
});
'use strict';

describe('Controller: ProgramManagementProgramSetupCtrl', function () {

  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var scope;

  var startIdNum = 111;
  function newId() { return 'id-'+startIdNum++; }

  var Grant = function(data){
    this.name = data.name;
    this.id = data.id;
  };

  Grant.prototype.$delete = jasmine.createSpy('Grant.$delete').and.callFake(function(callback) {
    callback()
  });

  Grant.prototype.$update = function(successFn, errFn) {
    /*jshint unused:false*/
    successFn(this);
  };

  var programs = [];

  function Program(name) {
    this.id = newId();
    this.name = name;
    this.grants = [];
  }

  Program.prototype.$delete = function(successFn, errFn) {
    var idx = programs.map(function(p){ return p.id; }).indexOf(this.id);
    if (idx !== -1) {
      programs.splice(idx,1);
      successFn();
    } else {
      errFn();
    }
  };

  Program.prototype.$update = function(successFn, errFn) {
    /*jshint unused:false*/
    successFn(this);
  };

  Program.prototype.addGrant = function(name) {
    var grant = new Grant({
      name: name,
      id:   newId()
    });
    this.grants.push(grant)
    return grant;
  };

  var Project = {
    getGrants: jasmine.createSpy('Project.getGrants').and.callFake(function(program, callback) {
      var grants = [];
      var idx = programs.map(function(p){ return p.id; }).indexOf(program.id);
      if (idx !== -1) grants = programs[idx].grants;
      callback(grants);
    }),
    query: jasmine.createSpy('Project.query').and.callFake(function(params, callback) {
      callback(programs);
    }),
    save: jasmine.createSpy('Project.save').and.callFake(function(params, callback) {
      var program = new Program( newId(), params.name);
      programs.push(program);
      callback(program);
    }),
    addGrant: jasmine.createSpy('Project.addGrant').and.callFake(function(params, callback, error) {
      var idx = programs.map(function(p){ return p.id; }).indexOf(params.projectId);
      if(idx != -1){
        var grant = programs[idx].addGrant(params.name);
        callback(grant);
      } else {
        error({errors: ['some error']});
      }

    })
  };

  var MockModal = {
    open: jasmine.createSpy('$modal.open').and.callFake(function() {
      return {
        result: { then: function(callback) {callback()} }
      }
    })
  };

  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    programs = [
      new Program('A'),
      new Program('B'),
      new Program('C')
    ];
    programs[0].addGrant('a');
    programs[0].addGrant('b');
    programs[1].addGrant('c');
    programs[1].addGrant('d');
    programs[2].addGrant('e');
    programs[2].addGrant('f');

    $controller('ProgramManagementProgramSetupCtrl', {
      $scope: scope,
      Project: Project,
      Grant: Grant,
      $modal: MockModal
    });
    Project.save.calls.reset();
    Project.addGrant.calls.reset();
    MockModal.open.calls.reset();
    Grant.prototype.$delete.calls.reset();

  }));

  it('should load $scope.programs on init', function(){
    expect(scope.programs).toBe(programs);
  });

  it('$scope.addGrant should push empty grant to $scope.grants', function(){
    scope.addGrant();
    var last_grant = scope.grants[scope.grants.length-1];
    expect(last_grant.name).toBe('');
    expect(last_grant.id).toBeUndefined();
  });

  describe('$scope.reset', function(){

    it('should clear form for new project', function () {
      scope.selectedProgram = {name: 'mmm'};
      scope.grants = [{name:'mmm'}];

      scope.reset();

      expect(scope.selectedProgram.name).toBe('');
      expect(scope.grants[0].name).toBe('');
    });

    it('should reset form for old project', function () {
      scope.selectedProgram = angular.copy(programs[1]);

      scope.reset();

      expect(scope.grants).toEqual(programs[1].grants);
    });

  });

  it('$scope.getGrants should get grunts for specified program', function () {
    scope.getGrants(programs[1]);
    expect(Project.getGrants).toHaveBeenCalled();
    expect(scope.grants).toEqual(programs[1].grants);
  });

  describe('$scope.add', function () {

    it('should show error if two grants has same name', function () {
      scope.grants = [ {name: 'asd'}, {name: 'asd'}, {id: 'asdasd'} ];
      scope.add();
      expect(scope.errors.length).toBe(1);
    });

    it('should create project if it is new and then add grants', function () {
      scope.grants = [ {name: 'G1'}, {name: 'G2'} ];
      scope.selectedProgram = {name: 'newProgram'};

      scope.add();

      expect(scope.errors.length).toBe(0);
      expect(Project.save).toHaveBeenCalled();
      expect(Project.addGrant).toHaveBeenCalledTimes(2);
      expect(scope.success).not.toBe('');
    });

    it('should add new grants only if project is NOT new', function () {
      scope.selectedProgram = angular.copy(programs[1]);
      scope.grants = angular.copy(programs[1].grants);
      scope.grants.push({name:'new'});

      scope.add();

      expect(scope.errors.length).toBe(0);
      expect(scope.success).not.toBe('');
      expect(Project.save).not.toHaveBeenCalled();
      expect(Project.addGrant).toHaveBeenCalledTimes(1);
    });

  });

  describe('$scope.deleteProgram', function(){

    it('should delete selected program from $scope.programs', function(){
      scope.selectedProgram = angular.copy(programs[1]);
      var deleted_program = programs[1];
      var saved_program = programs[2];

      scope.deleteProgram();

      expect(MockModal.open).toHaveBeenCalled();
      expect(scope.programs).not.toContain(deleted_program);
      expect(scope.programs).toContain(saved_program);
    });

    it('should clear selected program, grants, errors and messages', function(){
      scope.selectedProgram = programs[1];

      scope.deleteProgram();

      expect(scope.selectedProgram).toEqual({name:''});
      expect(scope.grants).toEqual([{name:''}]);
      expect(scope.success).toEqual('');
      expect(scope.errors).toEqual([]);
    })
  });

  describe('$scope.deleteGrant', function(){

    it('should delete grant from grants', function(){
      scope.selectedProgram = angular.copy(programs[1]);
      var grants = angular.copy(programs[1].grants);
      var saved_grant = grants[0];
      var deleted_grant = grants[1];

      scope.deleteGrant(grants,1);

      expect(MockModal.open).toHaveBeenCalled();
      expect(grants).toContain(saved_grant);
      expect(grants).not.toContain(deleted_grant);
    });

    it('should delete grant from $scope.programs', function(){
      scope.selectedProgram = angular.copy(programs[1]);
      var grants = angular.copy(programs[1].grants);
      var saved_grant = grants[0];
      var deleted_grant = grants[1];

      scope.deleteGrant(grants,1);

      expect(Grant.prototype.$delete).toHaveBeenCalledTimes(1);
      expect(scope.programs[1].grants).toContain(saved_grant);
      expect(scope.programs[1].grants).not.toContain(deleted_grant);

    });

    it('should do not call Grant.$delete if grant doesn\'t have id ', function(){
      scope.selectedProgram = programs[1];
      var grants = programs[1].grants;
      var idx = grants.push({name:'new'})-1;

      scope.deleteGrant(grants,idx);

      expect(Grant.prototype.$delete).not.toHaveBeenCalled();
      expect(grants[idx]).toBeUndefined()

    })
  })

});

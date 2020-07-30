'use strict';

describe('Controller: ProgramManagementAssignmentByUserCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    //mockServerCalls($provide);
  }));

  var ProgramManagementAssignmentByUserCtrl,
      scope, Project, User;

  var users = [
    {guid: 'a1', name: 'a-1', dn: 'a 1', permissions: []},
    {guid: 'a2', name: 'a-2', dn: 'a 2', permissions: []},
    {guid: 'a3', name: 'a-3', dn: 'a 3', permissions: []},
    {guid: 'b1', name: 'b-1', dn: 'b 1', permissions: []},
    {guid: 'b2', name: 'b-2', dn: 'b 2', permissions: []}
  ];

  var programs = [
    { id: 'p1',
      name: 'p-1',
      grants: [
        {id: 'p1g1', name: 'p-1 g-1'},
        {id: 'p1g2', name: 'p-1 g-2'}
      ]
    },
    { id: 'p2',
      name: 'p-2',
      grants: [
        {id: 'p2g1', name: 'p-2 g-1'},
        {id: 'p2g2', name: 'p-2 g-2'}
      ]
    }
  ];

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {

    scope = $rootScope.$new();

    Project = {

      query: jasmine.createSpy('query')
        .and.callFake(function(params, callback) {
          callback(programs);
        }),

      getGrants: jasmine.createSpy('getGrants')
        .and.callFake(function(program_obj, callback) {
          var program_grants = [];
          if (program_obj.id === programs[0].id ) program_grants = programs[0].grants;
          callback(program_grants);
        }),

      getUserPrograms: jasmine.createSpy('getUserPrograms')
        .and.callFake(function(params, callback) {
          callback([]);
        }),

      getUserProgramGrants: jasmine.createSpy('getUserProgramGrants')
        .and.callFake(function(params, callback) {
          callback([]);
        }),

      grantProgramPermissions: jasmine.createSpy('grantProgramPermissions')
        .and.callFake(function(arg, access, callback) {
          callback();
        }),

      revokeProgramPermissions: jasmine.createSpy('revokeProgramPermissions')
        .and.callFake(function(arg, access, callback) {
          callback();
        }),

      grantGrantPermissions : jasmine.createSpy('grantGrantPermissions')
        .and.callFake(function(arg, access, callback) {
          callback();
        }),

      revokeGrantPermissions: jasmine.createSpy('revokeGrantPermissions')
        .and.callFake(function(arg, access, callback) {
          callback();
        })
    };

    User = {
      list: jasmine.createSpy('list')
        .and.callFake(function(callback) {
          callback(users);
        })
    };

    ProgramManagementAssignmentByUserCtrl = $controller('ProgramManagementAssignmentByUserCtrl', {
      $scope: scope,
      Project: Project,
      User: User
    });

  }));

  it('should load users and programs on init', function () {
    expect(Project.query).toHaveBeenCalled();
    expect(User.list).toHaveBeenCalled();
    expect(scope.allUsers).toBe(users);
    expect(scope.programs).toBe(programs);
    
  });

  // common ini-state:
  // all programs and gramts are not selected and not changed
  // programs are not expanded

  describe('$scope.toggleProgram', function () {

    // click on non-selected program after page load
    it('should set program.selected to true and expand it, grants to true and disable them', function () {

      scope.toggleProgram(scope.programs[0]);
      expect(scope.programs[0].selected).toBe(true);
      expect(scope.programs[0].expanded).toBe(true);
      expect(scope.changedPrograms[programs[0].id]).toBe(true);
      expect(scope.changedPrograms[programs[1].id]).not.toBeDefined();

      var grants = scope.programs[0].grants;
      expect(scope.changedGrants[grants[0].id]).toBe(true);
      expect(scope.changedGrants[grants[1].id]).toBe(true);
      expect(scope.disabledGrants[grants[0].id]).toBe(true);
      expect(scope.disabledGrants[grants[1].id]).toBe(true);

    });

    // click on selected program after page load
    it('should set program.selected to false (grants to false and disable them)', function () {

      scope.programs[0].selected = true;
      scope.selectGrants(scope.programs[0], scope.programs[0].grants);
      scope.toggleProgram(scope.programs[0]);
      expect(scope.programs[0].selected).toBe(false);
      expect(scope.programs[0].expanded).toBe(false);
      expect(scope.changedPrograms[programs[0].id]).toBe(false);
      expect(scope.changedPrograms[programs[1].id]).not.toBeDefined();

      var grants = scope.programs[0].grants;
      expect(scope.changedGrants[grants[0].id]).toBe(false);
      expect(scope.changedGrants[grants[1].id]).toBe(false);
      expect(scope.disabledGrants[grants[0].id]).toBe(false);
      expect(scope.disabledGrants[grants[1].id]).toBe(false);

    });

    // click on non-selected program after page load
    // then click on program again
    it('should set program.selected to false (grants to true and enable them)', function () {

      scope.toggleProgram(scope.programs[0]);
      scope.toggleProgram(scope.programs[0]);
      expect(scope.programs[0].selected).toBe(false);
      expect(scope.programs[0].expanded).toBe(true);
      expect(scope.changedPrograms[programs[0].id]).toBe(false);
      expect(scope.changedPrograms[programs[1].id]).not.toBeDefined();

      var grants = scope.programs[0].grants;
      expect(scope.changedGrants[grants[0].id]).toBe(true);
      expect(scope.changedGrants[grants[1].id]).toBe(true);
      expect(scope.disabledGrants[grants[0].id]).toBe(false);
      expect(scope.disabledGrants[grants[1].id]).toBe(false);

    });

  });

  describe('$scope.isProgramSelected', function () {
    it('should report program select state right', function(){
      expect(scope.isProgramSelected(programs[0])).toBe(false);
      scope.toggleProgram(programs[0]);
      expect(scope.isProgramSelected(programs[0])).toBe(true);
    });
  });

  describe('$scope.toggleGrant', function () {
    it('should toggle grant state in scope.changedGrants', function(){
      var tst_grant = programs[1].grants[1];
      expect(scope.changedGrants[tst_grant.id]).not.toBeDefined();
      scope.toggleGrant(tst_grant);
      expect(scope.changedGrants[tst_grant.id]).toBe(true);
      scope.toggleGrant(tst_grant);
      expect(scope.changedGrants[tst_grant.id]).toBe(false);
    });
  });

  describe('$scope.isGrantSelected', function () {
    it('should report grant select state right', function(){
      // call selectGrants to set grant.selected states
      scope.selectGrants(programs[1],[]);
      var tst_grant = programs[1].grants[1];
      expect(scope.isGrantSelected(tst_grant)).toBe(false);
      scope.toggleGrant(tst_grant);
      expect(scope.isGrantSelected(tst_grant)).toBe(true);
    });
  });

  describe('$scope.toggleReadOnlyProgram', function () {

    it('select RO tag for program should select program '+
      'and grants and set RO tags for grants', function(){
      scope.toggleReadOnlyProgram(programs[1]);
      expect(scope.isProgramSelected(programs[1])).toBe(true);
      expect(scope.readOnlyPrograms[programs[1].id]).toBe(true);

      expect(scope.isGrantSelected(programs[1].grants[0])).toBe(true);
      for( var i=1; i< programs[1].grants.length; i++) {
        var grant_id = programs[1].grants[i].id;
        expect(scope.readOnlyGrants[grant_id]).toBe(true);
      }
    });

    it('unselect RO tag for program should change program RO state only',
      function () {
        scope.toggleReadOnlyProgram(programs[1]);
        scope.toggleReadOnlyProgram(programs[1]);
        expect(scope.isProgramSelected(programs[1])).toBe(true);
        expect(scope.readOnlyPrograms[programs[1].id]).toBe(false);

        expect(scope.isGrantSelected(programs[1].grants[0])).toBe(true);
        for (var i = 0; i < programs[1].grants.length; i++) {
          var grant_id = programs[1].grants[i].id;
          expect(scope.readOnlyGrants[grant_id]).toBe(true);
        }
      });

  });

  describe('$scope.toggleReadOnlyGrant', function () {
    it('should toggle RO grant state and select grant', function(){
      var tst_grant = programs[1].grants[1];
      expect(scope.changedGrants[tst_grant.id]).not.toBeDefined();

      scope.toggleReadOnlyGrant(tst_grant);
      expect(scope.changedGrants[tst_grant.id]).toBe(true);
      expect(scope.readOnlyGrants[tst_grant.id]).toBe(true);

      scope.toggleReadOnlyGrant(tst_grant);
      expect(scope.changedGrants[tst_grant.id]).toBe(true);
      expect(scope.readOnlyGrants[tst_grant.id]).toBe(false);
    });
  });

  describe('$scope.getAndSelectSelectedGrants', function () {

    // get grants for program.selected = false
    it('should expand program and enable grants', function () {
      scope.programs[0].selected = true;
      scope.getAndSelectSelectedGrants(scope.programs[0]);
      expect(Project.getGrants)
        .toHaveBeenCalledWith( {id:programs[0].id}, jasmine.any(Function), jasmine.any(Function));
      expect(scope.programs[0].selected).toBe(true);
      expect(scope.programs[0].expanded).toBe(true);

      var grants = scope.programs[0].grants;
      expect(scope.disabledGrants[grants[0].id]).toBe(true);
      expect(scope.disabledGrants[grants[1].id]).toBe(true);
    });

  });

  describe('savePermissions', function(){

    var tst_user, tst_program, tst_grants;
    beforeEach(function(){
      tst_user = users[0]; // default selected on page load
      tst_program = programs[0];
      tst_grants = tst_program.grants;
    });

    it('select a program '+
      'should grant RW access on program and grants and delete states',
      function () {
        scope.toggleProgram(tst_program);
        scope.savePermissions();

        var prg_arg = {id: tst_program.id, userId: tst_user.guid};
        expect(Project.grantProgramPermissions)
          .toHaveBeenCalledWith(
            prg_arg, ['READ', 'WRITE'],
            jasmine.any(Function), jasmine.any(Function)
          );
        // changedPrograms state should be deleted
        expect(scope.changedPrograms[tst_program.id]).not.toBeDefined();

        for (var i = 0; i < tst_grants.length; i++) {
          var grant_arg = {id: tst_grants[i].id, userId: tst_user.guid};
          // changedGrants states should be deleted
          expect(scope.changedGrants[tst_grants[i].id]).not.toBeDefined();
          // and grants should be selected
          expect(scope.isGrantSelected(tst_grants[i])).toBe(true);
          expect(Project.grantGrantPermissions)
            .toHaveBeenCalledWith(
              grant_arg, ['READ', 'WRITE'],
              jasmine.any(Function), jasmine.any(Function)
            );
        }
      }
    );

    it('unselect selected program '+
      'should revoke access on program and grants and delete states',
      function () {
        tst_program.selected = true;
        scope.selectGrants(tst_program, tst_grants);
        scope.toggleProgram(tst_program);

        scope.savePermissions();
        var prg_arg = {id: tst_program.id, userId: tst_user.guid};
        expect(Project.revokeProgramPermissions)
          .toHaveBeenCalledWith(
            prg_arg, ['READ', 'WRITE'],
            jasmine.any(Function), jasmine.any(Function)
          );
        // changedPrograms state should be deleted
        expect(scope.changedPrograms[tst_program.id]).not.toBeDefined();

        for (var i = 0; i < tst_grants.length; i++) {
          var grant_arg = {id: tst_grants[i].id, userId: tst_user.guid};
          // changedGrants states should be deleted
          expect(scope.changedGrants[tst_grants[i].id]).not.toBeDefined();
          // and grants should be unselected
          expect(scope.isGrantSelected(tst_grants[i])).toBe(false);
          expect(Project.revokeGrantPermissions)
            .toHaveBeenCalledWith(
              grant_arg, ['READ', 'WRITE'],
              jasmine.any(Function), jasmine.any(Function)
            );
        }

      }
    );
  })

});

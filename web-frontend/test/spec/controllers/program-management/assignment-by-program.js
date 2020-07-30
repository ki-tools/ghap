'use strict';

describe('Controller: ProgramManagementAssignmentByProgramCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ProgramManagementAssignmentByProgramCtrl,
      scope,
      Project,
      User;

  var grants = [ {name: 'a'}, {name: 'b'} ];
  var projects = [ {id: 'p1', name: 'p-1'}, {id: 'p2', name: 'p-2'} ];
  var users = [
    {guid: 'a1', name: 'a-1', dn: 'a 1', permissions: []},
    {guid: 'a2', name: 'a-2', dn: 'a 2', permissions: []},
    {guid: 'a3', name: 'a-3', dn: 'a 3', permissions: []},
    {guid: 'b1', name: 'b-1', dn: 'b 1', permissions: []},
    {guid: 'b2', name: 'b-2', dn: 'b 2', permissions: []}
  ];

  var testGrant = {id:'111-111-111', name:'Test Grant'};
  var testProgram = {id:'111-111-111', name:'Test Program'};

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    Project = {
      query: jasmine.createSpy('query').and.callFake(function(params, callback) {
        callback(projects);
      }),
      getProgramUsers: jasmine.createSpy('getProgramUsers').and.callFake(function(params, callback, error) {
        if(params.id){
          // callback get selected users list
          callback([]);
        } else {
          error({});
        }
      }),
      getGrants: jasmine.createSpy('Project.getGrants').and.callFake(function(params, callback) {
        callback(grants);
      }),
      getProgramGrantUsers: jasmine.createSpy('getProgramUsers'),
      grantProgramPermissions: jasmine.createSpy('grantProgramPermissions'),
      revokeProgramPermissions: jasmine.createSpy('revokeProgramPermissions'),
      grantGrantPermissions : jasmine.createSpy('grantGrantPermissions'),
      revokeGrantPermissions: jasmine.createSpy('revokeGrantPermissions')
    };
    User = {
      list: jasmine.createSpy('User.list').and.callFake(function(callback) {
        callback(users);
      })
    };
    ProgramManagementAssignmentByProgramCtrl = $controller('ProgramManagementAssignmentByProgramCtrl', {
      $scope: scope,
      Project: Project,
      User: User
    });
  }));

  it('should call Project.query on load', function () {
    expect(Project.query).toHaveBeenCalled();
    expect(User.list).toHaveBeenCalled();
  });


  // >>>>>>> QA note:
  // $scope.changedUsers is declared in the controller as an array but used as an object
  // if variable declared as  var myArray =[]
  // then in IE operator myArray['key'] = value will create a new array item
  // but in fireFox and in NodeJS this operator will add property 'key' to the myArray object
  // The savePermissions methods using for over properties iterator to provide correct functionality.

  it('should track selected users', function() {
    for(var i=0; i< users.length; i++) {
      expect(scope.isUserSelected(users[i])).toBe(false);
      expect(scope.changedUsers[users[i].guid]).not.toBeDefined();
      expect(scope.readOnlyUsers[users[i].guid]).not.toBeDefined();
    }
    scope.toggleUser(users[3]);
    expect(scope.isUserSelected(users[3])).toBe(true);
    scope.toggleUser(users[3]);
    expect(scope.changedUsers[users[3].guid]).toBeDefined();
    expect(scope.isUserSelected(users[3])).toBe(false);

    // validate readOnly trackers

    scope.toggleReadOnlyUser(users[1]);
    expect(scope.isUserSelected(users[1])).toBe(true);
    expect(scope.readOnlyUsers[users[1].guid]).toBe(true);

    scope.toggleReadOnlyUser(users[1]);
    expect(scope.isUserSelected(users[1])).toBe(true);
    expect(scope.readOnlyUsers[users[1].guid]).toBeDefined();
    expect(scope.readOnlyUsers[users[1].guid]).toBe(false);

  });

  it('should collect httpResponse errors', function(){
    scope.collectErrors({});
    expect(scope.errors.length).toBe(0);
    var msg1 = 'first error message';
    var msg2 = 'second error message';
    var httpResponse = {
      data: {errors:[{msg:msg1}, {msg:msg2}]}
    };
    scope.collectErrors(httpResponse);
    expect(scope.errors.length).toBe(2);
    expect(scope.errors[0]).toBe(msg1);
    expect(scope.errors[1]).toBe(msg2);
  });

  describe('$scope.savePermissions', function(){

    var spySavePermissionsProject;
    var spySavePermissionsGrant;

    beforeEach(function(){
      scope.resetSelection();
      spySavePermissionsProject = spyOn(scope,'savePermissionsProject');
      spySavePermissionsGrant = spyOn(scope,'savePermissionsGrant');
    });

    afterEach(function(){
      spySavePermissionsProject.and.callThrough();
      spySavePermissionsGrant.and.callThrough();
    });

    it('should call savePermissionsProjects for the selected program', function(){
      scope.selectedProgram = testProgram;
      scope.savePermissions();
      expect(scope.savePermissionsProject).toHaveBeenCalledWith(testProgram);
      expect(scope.savePermissionsGrant).not.toHaveBeenCalled();
    });

    it('should call savePermissionsGrants for the selected grant', function(){
      scope.selectedGrant = testGrant;
      scope.savePermissions();
      expect(scope.savePermissionsProject).not.toHaveBeenCalled();
      expect(scope.savePermissionsGrant).toHaveBeenCalledWith(testGrant);
    })

  });

  describe('$scope.savePermissionsProject', function(){

    var permissionsArg;
    beforeEach(function(){
      scope.resetSelection();
      permissionsArg = {id: testProgram.id, userId: users[0].guid};
    });

    it('should grant RW access on selected user', function(){
      scope.toggleUser(users[0]);
      scope.savePermissionsProject(testProgram);
      expect(Project.grantProgramPermissions)
          .toHaveBeenCalledWith(permissionsArg, ['READ', 'WRITE'],
              jasmine.any(Function), jasmine.any(Function));
    });

    it('should grant RO access on selected user', function(){
      scope.toggleReadOnlyUser(users[0]);
      var grant_arg = {id: testProgram.id, userId: users[0].guid};
      scope.savePermissionsProject(testProgram);
      expect(Project.grantProgramPermissions)
          .toHaveBeenCalledWith(permissionsArg, ['READ'],
              jasmine.any(Function), jasmine.any(Function));
    });

    it('should revoke RW access on selected user', function(){
      users[0].selected = true;
      scope.toggleUser(users[0]);
      var grant_arg = {id: testProgram.id, userId: users[0].guid};
      scope.savePermissionsProject(testProgram);
      expect(Project.revokeProgramPermissions)
          .toHaveBeenCalledWith(permissionsArg, ['READ', 'WRITE'],
              jasmine.any(Function), jasmine.any(Function));
    });

    it('should revoke RW access even if RO tag present', function(){
      scope.toggleReadOnlyUser(users[0]);
      scope.toggleUser(users[0]);
      var grant_arg = {id: testProgram.id, userId: users[0].guid};
      scope.savePermissionsProject(testProgram);
      expect(Project.revokeProgramPermissions)
          .toHaveBeenCalledWith(permissionsArg, ['READ', 'WRITE'],
              jasmine.any(Function), jasmine.any(Function));
    })

  });

  describe('$scope.savePermissionsGrant', function(){

    beforeEach(function(){
      scope.changedUsers = {'usr-1':false, 'usr-3':true, 'usr-5':true, 'usr-2':false};
      scope.readOnlyUsers = {'usr-3':false, 'usr-5':true, 'user-2':true};
      scope.savePermissionsGrant(testGrant);
    });

    it('should revoke RW access', function(){
      var revoke_arg = {id: testGrant.id, userId:'usr-1'};
      expect(Project.revokeGrantPermissions).toHaveBeenCalledWith( revoke_arg, ['READ', 'WRITE'], jasmine.any(Function), jasmine.any(Function));
    });

    it('should grant RW access', function(){
      var grant_arg = {id: testGrant.id, userId:'usr-3'};
      expect(Project.grantGrantPermissions).toHaveBeenCalledWith( grant_arg, ['READ', 'WRITE'], jasmine.any(Function), jasmine.any(Function));
    });

    it('should grant RO access', function(){
      var grant_arg = {id: testGrant.id, userId:'usr-5'};
      expect(Project.grantGrantPermissions).toHaveBeenCalledWith( grant_arg, ['READ'], jasmine.any(Function), jasmine.any(Function));
    });

    it('should revoke RW access even if RO tag present', function(){
      // read-only tag should be ignored if access to grant set to disabled
      var grant_arg = {id: testGrant.id, userId:'usr-2'};
      expect(Project.revokeGrantPermissions).toHaveBeenCalledWith( grant_arg, ['READ', 'WRITE'], jasmine.any(Function), jasmine.any(Function));
    })

  });

  describe('$scope.getGrantUsers', function(){

    it('should reset selection and set selected grant', function(){
      spyOn(scope,'resetSelection');
      scope.getGrantUsers(testGrant);
      expect(scope.resetSelection).toHaveBeenCalled();
      expect(scope.selectedGrant).toBe(testGrant);
    });

    it('should call Project.getProgramGrantUsers', function(){
      scope.getGrantUsers(testGrant);
      expect(Project.getProgramGrantUsers).toHaveBeenCalled();
    })

  })

});

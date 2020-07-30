'use strict';

describe('Controller: UserManagementEditAccountCtrl', function () {

  var lastModalInstance;
  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
    decorateModal($provide, function(modalInstance){
      lastModalInstance = modalInstance;
    });
  }));

  var UserManagementEditAccountCtrl,
      scope, User, PersonalStorage,
      users, currentUser;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope, $modal, $filter) {
    users = [
      { name: 'user1', dn: 'dn1', guid: '1' },
      { name: 'user2', dn: 'dn2', guid: '2' }
    ];
    currentUser = users[1];

    scope = $rootScope.$new();
    User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(function(callback) {
        callback(currentUser);
      }),
      list: jasmine.createSpy('list').and.callFake(function(success, error) {
        if (currentUser)
          success(users);
        else
          error({data: 'some error'})
      }),
      get: jasmine.createSpy('get').and.callFake(function(userData, success, error) {
        if(userData.dn){
          success(userData);
        } else {
          error({data: 'some error'});
        }
      })
    };
    PersonalStorage = {
      exists: jasmine.createSpy('PersonalStorage.exists').and.callFake(function(params, success) {
        success();
      }),
      create: jasmine.createSpy('PersonalStorage.create').and.callFake(function(params, success) {
        success({});
      }),
      delete: jasmine.createSpy('PersonalStorage.delete').and.callFake(function(params, success) {
        success({});
      })
    };
    UserManagementEditAccountCtrl = $controller('UserManagementEditAccountCtrl', {
      $scope: scope,
      User: User,
      PersonalStorage: PersonalStorage,
      $modal: $modal,
      $filter: $filter
    });
  }));

  it('should show error for wrong User.get response', function () {
    scope.get({}, 0);
    expect(scope.errors.length).toBe(1);
  });

  it('should show error for wrong User.list response', inject(function ($controller) {
    currentUser = null;
    expect(scope.errors.length).toBe(0);
    UserManagementEditAccountCtrl = $controller('UserManagementEditAccountCtrl', {
      $scope: scope, User: User
    });
    expect(scope.errors.length).toBe(1);
  }));

  describe('scope.changeStorage ', function () {

    it('should create PersonalStorage if it was enabled', function () {
      scope.selectedUser.storage = true;
      scope.changeStorage();
      expect(PersonalStorage.create).toHaveBeenCalled();
    });

    it('should delete PersonalStorage if it was disabled', function () {
      scope.selectedUser.storage = false;
      spyOn(window, 'confirm').and.returnValue(true);
      scope.changeStorage();
      expect(PersonalStorage.delete).toHaveBeenCalled();
    });

  });

  describe('$scope.update', function () {

    var updatedUser;
    beforeEach(function(){
      scope.selectedUser = users[1];
      updatedUser = angular.copy(users[1]);
      updatedUser.name = 'updated user';
      User.save = jasmine.createSpy('User.save').and.callFake(function(params, successCallback) {
        successCallback(updatedUser);
      });
    });

    it('should call PersonalStorage.exists after user update', function () {

      scope.update();

      expect(scope.users[1].name).toBe(updatedUser.name);
      expect(scope.errors.length).toBe(0);
      expect(scope.selectedUser.storage).toBe(true);

      expect(PersonalStorage.exists).toHaveBeenCalledWith({guid: updatedUser.guid}, jasmine.any(Function));
    });

    it('should show error if password defined but do not match password confirmation', function(){
      scope.selectedUser.password = 'new';
      scope.update();
      expect(scope.errors.length).toBe(1);
      expect(scope.errors[0]).toContain('Invalid password confirmation');

    })

  });

  describe("$scope.deleteUser", function(){

    var $httpBackend, $modal;
    beforeEach(inject(function(_$httpBackend_, _$modal_){
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
      $httpBackend.expectGET('undefined/current?token=null').respond(200,'[{}]');
      $httpBackend.expectGET('views/user-management/delete-confirm.html').respond(200,'<forn></forn>');
      $httpBackend.expectGET('views/not-modal/window.html').respond(200,'<div></div>');
      $modal = _$modal_;
      spyOn($modal,'open').and.callThrough();
    }));


    it('should open modal dialog to confirm deletion', function(){
      scope.delete();
      expect($modal.open).toHaveBeenCalled();
    });

    it('should delete selected user from $scope.users', inject(function ($controller, $rootScope) {
      scope.selectedUser = users[1];
      scope.selectedUser.$delete = function(cb){cb()};

      scope.delete();

      $httpBackend.flush();
      scope.$digest();      // Propagate promise resolution

      // instead of call lastModalInstance.close();
      // i can do needless trick to test UserDeleteConfirmModalCtrl
      var modalScope = $rootScope.$new();
      $controller('UserDeleteConfirmModalCtrl',{
        $scope : modalScope,
        $modalInstance : lastModalInstance,
        user: users[1]
      });
      modalScope.del();
      scope.$digest();     // Propagate promise resolution

      expect(scope.users).not.toContain(scope.selectedUser);
      expect(scope.users).not.toContain(users[1]);
      expect(scope.users).toContain(users[0]);
      expect(scope.users.length).toBe(users.length-1);
    }));

  });

});

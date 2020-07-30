'use strict';

describe('Controller: VisualizationsSubmitCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp'));

  var VisualizationsSubmitCtrl,
      scope,
      User;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser'),
    };
    VisualizationsSubmitCtrl = $controller('VisualizationsSubmitCtrl', {
      $scope: scope,
      User: User,
    });
  }));

  it('should call User.getCurrentUser on load', function () {
    expect(User.getCurrentUser).toHaveBeenCalled();
  });

  it('$scope.initUserAndUpload should set user and call $scope.initUploader', function () {
    var user = {guid: 1};
    scope.initUploader = jasmine.createSpy('initUploader');

    scope.initUserAndUpload(user);


    expect(scope.user).toBe(user);
    expect(scope.initUploader).toHaveBeenCalledWith(user.guid);
  });

  describe('$scope.initUploader', function () {

    it('should initialize uploader', function () {
      scope.initUploader();
      expect(scope.appUploader).not.toBe(null);
    });

  });

  it('$scope.resetMessages should set error and appUploaded to false', function () {
    scope.resetMessages();
    expect(scope.errors.length).toBe(0);
    expect(scope.appUploaded).toBe(false);
  });

  it('$scope.setAppUploaded should set appUploaded to true', function () {
    scope.setAppUploaded();
    expect(scope.appUploaded).toBe(true);
  });

  it('$scope.setAppUploaded should set error to true', function () {
    scope.setError({}, {errors: [
      {
        field: 'name',
        errors: [{message: 'Some error message'}]
      }]
    });
    expect(scope.errors.length).toBe(1);
  });

});

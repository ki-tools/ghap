'use strict';

describe('Controller: FeedbackModalCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var FeedbackModalCtrl,
    scope,
    currUser,
    modalInstance,
    Report,
    $httpBackend,
    $timeout;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope, _$httpBackend_, _$timeout_) {
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;

    scope = $rootScope.$new();
    currUser = {guid: 1};  
    var User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(
        function(setUserAndGetReports) {
          setUserAndGetReports(currUser);
        }
      )
    };
    //var $timeout = function(callback, delta){
    //  callback(delta);
    //};
    modalInstance = {
      close:  jasmine.createSpy('close'),
      dismiss: jasmine.createSpy('dismiss')
    };
    FeedbackModalCtrl = $controller('FeedbackModalCtrl', {
      $scope: scope,
      User: User,
      $modalInstance: modalInstance,
      $timeout: $timeout
    });
    $httpBackend.expectGET('/locales/locale-en.json').respond(200, '{}');
    $httpBackend.expectGET('undefined/current?token=null').respond(200, '[{}]');
    $httpBackend.expectPOST('http://test-jira').respond(200, 'Success');

  }));


  it('init should call for current user', function () {
    expect(scope.user).toBe(currUser);
  });

  describe('$scope.submit', function () {
    it('should submit and close modal', function () {
      scope.submit();
      $httpBackend.flush();
      $timeout.flush(2000);
      expect(modalInstance.close).toHaveBeenCalled();
    });

  });

  it('dismiss should close modal', function () {
    scope.dismiss();
    expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
  });

});

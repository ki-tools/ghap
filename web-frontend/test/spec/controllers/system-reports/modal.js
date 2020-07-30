'use strict';

describe('Controller: ModalSystemReportsCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ModalSystemReportsCtrl,
    scope,
    currUser,
    modalInstance,
    Report;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    currUser = {guid: 1};  
    var User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(
        function(setUserAndGetReports) {
          setUserAndGetReports(currUser);
        }
      )
    };
    Report = {
      create: jasmine.createSpy('create')
    };
    modalInstance = {
      close:  jasmine.createSpy('close'),
      dismiss: jasmine.createSpy('dismiss')
    };
    ModalSystemReportsCtrl = $controller('ModalSystemReportsCtrl', {
      $scope: scope,
      User: User,
      $modalInstance: modalInstance,
      Report: Report,
      selectedReport: {constraintTypes:['DATE_RANGE']}
    });
  }));

  it('init should call for current user', function () {
    expect(scope.user).toBe(currUser);
  });

  describe('$scope.create', function () {

    it('should show error if date range fields are empty', function () {
      scope.create();
      expect(scope.error).toBe('Please select dates');
    });

    it('should show error if start date more than end date', function () {
      scope.start = 2;
      scope.end = 1;
      scope.create();
      expect(scope.error).toBe('Invalid date range');
    });

    it('should create report and close modal', function () {
      scope.start = 1;
      scope.end = 2;
      scope.create();
      expect(modalInstance.close).toHaveBeenCalled();
    });

  });

  it('cancel should close modal', function () {
    scope.cancel();
    expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
  });

});

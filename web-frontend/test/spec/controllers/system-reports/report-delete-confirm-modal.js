/**
 * Created by Vlad on 20.02.2016.
 */

'use strict';

describe('Controller: ReportDeleteConfirmModalCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ReportDeleteConfirmModalCtrl,
    scope,
    modalInstance;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    modalInstance = {
      close:  jasmine.createSpy('close'),
      dismiss: jasmine.createSpy('dismiss')
    };
    ReportDeleteConfirmModalCtrl = $controller('ReportDeleteConfirmModalCtrl', {
      $scope: scope,
      $modalInstance: modalInstance,
    });
  }));

  it('del should close modal dialog', function () {
    scope.del();
    expect(modalInstance.close).toHaveBeenCalled();
  });

  it('cancel should dismiss modal dialog', function () {
    scope.cancel();
    expect(modalInstance.dismiss).toHaveBeenCalled();
  });

});

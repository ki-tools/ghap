'use strict';

describe('Controller: ProgramManagementCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ProgramManagementCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ProgramManagementCtrl = $controller('ProgramManagementCtrl', {
      $scope: scope
    });
  }));

});

'use strict';

describe('Controller: RoleManagementCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var RoleManagementCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    RoleManagementCtrl = $controller('RoleManagementCtrl', {
      $scope: scope
    });
  }));

});

'use strict';

describe('Controller: AccessDeniedCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var AccessDeniedCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    AccessDeniedCtrl = $controller('AccessDeniedCtrl', {
      $scope: scope
    });
  }));
  
});

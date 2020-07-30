'use strict';

describe('Controller: PolicyCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var PolicyCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    PolicyCtrl = $controller('PolicyCtrl', {
      $scope: scope
    });
  }));
});

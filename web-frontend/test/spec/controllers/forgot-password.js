'use strict';

describe('Controller: ForgotPasswordCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ForgotPasswordCtrl,
    scope;

  var User = {
    save: function(data, url, success){
        var user = {name: 'testUser', dn: 'TestDn'};
        success(user);
    }
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ForgotPasswordCtrl = $controller('ForgotPasswordCtrl', {
      $scope: scope,
      User: User
    });
  }));

  it('$scope.restore should update "requestSent" status', function () {

    scope.userid = '';
    scope.requestSent = false;


    scope.restore();

    expect(scope.requestSent).toBe(true);
  });
});

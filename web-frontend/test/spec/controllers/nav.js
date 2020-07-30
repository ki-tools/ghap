'use strict';

describe('Controller: NavCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var NavCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    NavCtrl = $controller('NavCtrl', {
      $scope: scope
    });
  }));

  describe('$scope.isVisible', function () {

    it('should return true for not Viz items', function () {
      var item = {name: 'a'};
      expect(scope.isVisible(item)).toBe(true);
    });

    it('should return true for Viz item if we are in VPN', function () {
      var item = {name: 'Visualizations'};
      scope.isInVpn = true;
      expect(scope.isVisible(item)).toBe(true);
    });

    it('should return false for Viz item if we are not in VPN', function () {
      var item = {name: 'Visualizations'};
      scope.isInVpn = false;
      expect(scope.isVisible(item)).toBe(false);
    });

  });

});

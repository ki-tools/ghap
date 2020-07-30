'use strict';

describe('Directive: newPassword', function () {

  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  describe('NewPasswordDirectiveCtrl', function () {

    var NewPasswordDirectiveCtrl,
      scope;

    beforeEach(inject(function ($controller, $rootScope) {
      scope = $rootScope.$new();
      NewPasswordDirectiveCtrl = $controller('NewPasswordDirectiveCtrl', {
        $scope: scope
      });
    }));

    describe('$scope.checkConds()', function () {

      it('should do the check', inject(function () {
        //console.log($compile);
        scope.password = 'aa';
        scope.checkConds();
        expect(scope.hasUpperCase).toBe(false);
        expect(scope.hasLowCase).toBe(true);
        expect(scope.hasDigits).toBe(false);
        expect(scope.hasNonAlphas).toBe(false);
        expect(scope.moreThen7).toBe(false);
      }));

    });

  });

});

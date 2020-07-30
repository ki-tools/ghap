'use strict';

describe('Directive: sorter', function () {

  // load the directive's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<sorter></sorter>');
    element = $compile(element)(scope);
    //expect(element.text()).toBe('this is the sorter directive');
  }));
});

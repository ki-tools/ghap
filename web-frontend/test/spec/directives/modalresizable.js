'use strict';

describe('Directive: modalresizable', function () {

  // load the directive's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make modal dialog resizable', inject(function ($compile) {
    element = angular.element('<div modalresizable></div>');
    element = $compile(element)(scope);
    //expect(element.text()).toBe('this is the resizable directive');
  }));
});

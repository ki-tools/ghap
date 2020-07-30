'use strict';

describe('Service: Stack', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var Stack;
  beforeEach(inject(function (_Stack_) {
    Stack = _Stack_;
  }));

  it('should do something', function () {
    expect(!!Stack).toBe(true);
  });

});

'use strict';

describe('Service: focus', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var focus;
  beforeEach(inject(function (_focus_) {
    focus = _focus_;
  }));

  it('should do something', function () {
    expect(!!focus).toBe(true);
  });

});

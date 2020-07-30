'use strict';

describe('Service: BodyShifter', function () {

  // load the service's module
  beforeEach(module('bmgfApp'));

  // instantiate service
  var BodyShifter;
  beforeEach(inject(function (_BodyShifter_) {
    BodyShifter = _BodyShifter_;
  }));

  it('should do something', function () {
    expect(!!BodyShifter).toBe(true);
  });

});

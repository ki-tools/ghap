'use strict';

describe('Service: group', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var group;
  beforeEach(inject(function (_group_) {
    group = _group_;
  }));

});

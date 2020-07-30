'use strict';

describe('Service: Role', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var Role;
  beforeEach(inject(function (_Role_) {
    Role = _Role_;
  }));

});

'use strict';

describe('Service: ActivityRoleAssociation', function () {

  // load the service's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  // instantiate service
  var ActivityRoleAssociation;
  beforeEach(inject(function (_ActivityRoleAssociation_) {
    ActivityRoleAssociation = _ActivityRoleAssociation_;
  }));

  it('should do something', function () {
    expect(!!ActivityRoleAssociation).toBe(true);
  });

});

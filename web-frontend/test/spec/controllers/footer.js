/**
 * Created by Vlad on 10.03.2016.
 */

'use strict';

describe('Controller: FooterCtrl', function () {

  var lastModalInstance;
  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
    decorateModal($provide, function (modalInstance) {
      lastModalInstance = modalInstance;
    });
  }));

  var scope, $httpBackend, $modal;
  beforeEach(inject(function ($controller, $rootScope, _$modal_, _$httpBackend_) {

    scope = $rootScope.$new();

    $modal = _$modal_;
    spyOn($modal,'open').and.callThrough();

    $controller('FooterCtrl', {
      $scope: scope,
      $modal: $modal
    });

    $httpBackend = _$httpBackend_;

    $httpBackend.expectGET('/locales/locale-en.json').respond(200,'{}');
    $httpBackend.expectGET('undefined/current?token=null').respond(200,'[{}]');
    $httpBackend.expectGET('views/terms-modal.html').respond(200,'<div></div>');
  }));

  describe('$scope.showTerms', function(){

    it('should call $modal.open to open modal dialog', inject(function ($controller, $rootScope) {
      //jshint unused:false
      scope.showTerms();
      expect($modal.open).toHaveBeenCalled();
    }));

    it('should remove body-terms-modal class after show of modal', inject(function ($controller, $rootScope) {

      scope.showTerms();
      $httpBackend.flush();
      scope.$digest();      // Propagate promise resolution

      expect($('body').attr('class')).toContain('body-terms-modal');

      // instead of call lastModalInstance.dismiss();
      // i can do needless trick to test FooterModalCtrl
      var modalScope = $rootScope.$new();
      $controller('FooterModalCtrl',{
        $scope : modalScope,
        $modalInstance : lastModalInstance,
      });
      modalScope.dismiss();
      scope.$digest();     // Propagate promise resolution

      expect($('body').attr('class')).not.toContain('body-terms-modal');

    }));
  });

});
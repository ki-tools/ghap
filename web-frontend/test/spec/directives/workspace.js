'use strict';

describe('Directive: Workspace', function () {
  
  var WorkspaceCtrl;
  var scope;
  var modal;
  var UserData;

  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));
  
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    modal = {
      open: jasmine.createSpy('open').and.callFake(function() {
        return {
          result: {
            then: function(callback){
              callback();
            }
          }
        };
      }),
    };
    UserData = {
      remove: jasmine.createSpy('remove'),
    };
    WorkspaceCtrl = $controller('WorkspaceCtrl', {
      $scope:   scope,
      $timeout: function(callback){ callback(); },
      $modal: modal,
      UserData: UserData,
    });
  }));

  describe('$scope.onKeyPress', function () {

    it('should do nothing on keys that have code other then 13', function () {
      scope.hideAndCreateFolder = jasmine.createSpy('hideAndCreateFolder');
      scope.onKeyPress({which: 1});
      expect(scope.hideAndCreateFolder).not.toHaveBeenCalled();
    });

    it('should call hideAndCreateFolder on enter', function () {
      scope.hideAndCreateFolder = jasmine.createSpy('hideAndCreateFolder');
      scope.onKeyPress({which: 13});
      expect(scope.hideAndCreateFolder).toHaveBeenCalled();
    });

  });

  it('$scope.hideAndCreateFolder should call createFolder and hide dialog', function () {
  	scope.newFolderName = 'test';
    scope.createFolder = jasmine.createSpy('createFolder');
    scope.hideAndCreateFolder();
    expect(scope.createFolder).toHaveBeenCalledWith('test');
    expect(scope.isNewFolderDialogVisible).toBe(false);
  });

  describe('$scope.toggleNewFolderDialog', function () {

    it('should clear field and hide dialog if it was open', function () {
      scope.newFolderName = 'test';
      scope.isNewFolderDialogVisible = true;
      scope.toggleNewFolderDialog();
      expect(scope.newFolderName).toBe('');
      expect(scope.isNewFolderDialogVisible).toBe(false);
    });

    // it('should clear field, show dialog if it was hided and set focus', function () {
    //   scope.newFolderName = 'test';
    //   scope.isNewFolderDialogVisible = false;
    //   scope.toggleNewFolderDialog();

    //   var focus = jasmine.createSpy('focus');
    //   document.getElementById = function(id) {
    //   	console.log(id);
    //   	return {
    //   		focus: focus
    //   	};
    //   }

    //   expect(scope.newFolderName).toBe('');
    //   expect(scope.isNewFolderDialogVisible).toBe(true);
    //   expect(focus).toHaveBeenCalledWith();
    // });

  });

  describe('$scope.noSelection', function () {

    it('should return false if a file is selected', function () {
      scope.userData = [ {checked: true}, {id: 1}, {checked: null} ];
      expect(scope.noSelection()).toBe(false);
    });

    it('should return true if no files are selected', function () {
      scope.userData = [ {checked: false}, {id: 1}, {checked: null} ];
      expect(scope.noSelection()).toBe(true);
    });

  });

  describe('$scope.allFilesSelected', function () {

    it('should return true if all files are selected', function () {
      scope.userData = [ {checked: true}, {checked: true} ];
      expect(scope.allFilesSelected()).toBe(true);
    });

    it('should return false if some files are not selected', function () {
      scope.userData = [ {checked: true}, {checked: true}, {checked: null} ];
      expect(scope.allFilesSelected()).toBe(false);
    });

    it('should return false if there are no files', function () {
      scope.userData = [];
      expect(scope.allFilesSelected()).toBe(false);
    });

  });

  describe('$scope.toggleSelectAllFiles', function () {

    it('should select unselected files', function () {
      scope.userData = [ {checked: true}, {checked: false} ];
      scope.selectAllFiles = true;
      scope.toggleSelectAllFiles();
      expect(scope.userData[1].checked).toBe(true);
    });

  });

  describe('$scope.doFileAct', function () {

    it('should call scope.goTo and scope.downloadZipUrl if action is Download', function () {
      scope.action = 'Download';
      scope.goTo = jasmine.createSpy('goTo');
      scope.downloadZipUrl = jasmine.createSpy('downloadZipUrl');
      scope.doFileAct();

      expect(scope.goTo).toHaveBeenCalled();
      expect(scope.downloadZipUrl).toHaveBeenCalled();
      expect(scope.action).toBe('Actions');
    });

    it('should call $modal.open if action is Delete', function () {
      scope.action = 'Delete';
      scope.user = {guid: 1};
      scope.doFileAct();

      expect(modal.open).toHaveBeenCalled();
      expect(UserData.remove).toHaveBeenCalled();
    });

  });

  it('$scope.onUploadStart should set $scope.uploading to true', function () {
    scope.onUploadStart();
    expect(scope.uploading).toBe(true);
  });

  it('$scope.onUploadComplete should set $scope.uploading to false and call $scope.loadUserData', function () {
    scope.loadUserData = jasmine.createSpy('loadUserData');
    scope.onUploadComplete();
    expect(scope.uploading).toBe(false);
    expect(scope.loadUserData).toHaveBeenCalled();
  });

});

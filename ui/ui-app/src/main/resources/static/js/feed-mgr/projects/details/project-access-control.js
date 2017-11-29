define(['angular', 'feed-mgr/projects/module-name'], function (angular, moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {
                stepIndex: '@'
            },
            scope: {},
            controllerAs: 'vm',
            templateUrl: 'js/feed-mgr/projects/details/project-access-control.html',
            controller: "ProjectAccessControlController",
            link: function ($scope, element, attrs, controller) {
            }
        };
    };

    function ProjectAccessControlController($scope, $q, $mdToast, ProjectsService, AccessControlService, EntityAccessControlService) {

        /**
         * ref back to this controller
         * @type {TemplateAccessControlController}
         */
        var self = this;

        this.projectAccessControlForm = {};

        this.model = ProjectsService.model;

        if (ProjectsService.model.roleMemberships == undefined) {
            ProjectsService.model.roleMemberships = this.model.roleMemberships = [];
        }

        /*if (ProjectsService.model.feedRoleMemberships == undefined) {
        	ProjectsService.model.feedRoleMemberships = this.model.feedRoleMemberships = [];
        }*/

        /**
         * Indicates if the properties may be edited.
         */
        self.allowEdit = false;

        /**
         * Project data used in "edit" mode.
         * @type {ProjectModel}
         */
        self.editModel = ProjectsService.newProject();

        /**
         * Indicates if the view is in "edit" mode.
         * @type {boolean} {@code true} if in "edit" mode or {@code false} if in "normal" mode
         */
        self.isEditable = false;

        /**
         * Indicates if the project is new.
         * @type {boolean}
         */
        self.isNew = true;

        $scope.$watch(
            function () {
                return ProjectsService.model.id
            },
            function (newValue) {
                self.isNew = !angular.isString(newValue)
            }
        );

        /**
         * Project data used in "normal" mode.
         * @type {ProjectModel}
         */
        self.model = ProjectsService.model;

        /**
         * Switches to "edit" mode.
         */
        self.onEdit = function () {
            self.editModel = angular.copy(self.model);
        };

        /**
         * Saves the project .
         */
        self.onSave = function () {
            var model = angular.copy(ProjectsService.model);
            model.roleMemberships = self.editModel.roleMemberships;
            //model.feedRoleMemberships = self.editModel.feedRoleMemberships;
            model.owner = self.editModel.owner;
            EntityAccessControlService.updateRoleMembershipsForSave(model.roleMemberships);
            //EntityAccessControlService.updateRoleMembershipsForSave(model.feedRoleMemberships);

            //TODO Open a Dialog showing Project is Saving progress
            //ProjectsService.save(model).then(function (response) {
            ProjectsService.doUpdate(model).then(function (response) {
                self.model = ProjectsService.model = response.data;
                //set the editable flag to false after the save is complete.
                //this will flip the directive to read only mode and call the entity-access#init() method to requery the accesss control for this entity
                self.isEditable = false;
                ProjectsService.update(response.data);
                $mdToast.show(
                    $mdToast.simple()
                        .textContent('Saved the Project')
                        .hideDelay(3000)
                );
            }, function (err) {
                //keep editable active if an error occurred
                self.isEditable = true;
                $mdDialog.show(
                    $mdDialog.alert()
                        .clickOutsideToClose(true)
                        .title("Save Failed")
                        .textContent("The project '" + model.projectName + "' could not be saved. " + err.data.message)
                        .ariaLabel("Failed to save project")
                        .ok("Got it!")
                );
            });
        };

        //Apply the entity access permissions
        $q.when(AccessControlService.hasPermission(AccessControlService.PROJECTS_EDIT,self.model,AccessControlService.ENTITY_ACCESS.PROJECT.CHANGE_PROJECT_PERMISSIONS)).then(function(access) {
            self.allowEdit = access;
        });

    }

    angular.module(moduleName).controller("ProjectAccessControlController",
        ["$scope", "$q", "$mdToast", "ProjectsService", "AccessControlService", "EntityAccessControlService", ProjectAccessControlController]);

    angular.module(moduleName).directive("thinkbigProjectAccessControl", directive);
});


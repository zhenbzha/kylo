define(['angular','feed-mgr/projects/module-name'], function (angular,moduleName) {

    /**
     * Displays a list of projects.
     *
     * @constructor
     * @param $scope the application model
     * @param {AccessControlService} AccessControlService the access control service
     * @param AddButtonService the Add button service
     * @param ProjectsService the projects service
     * @param StateService the page state service
     */
    var ProjectsController = function($scope, AccessControlService, AddButtonService, ProjectsService, StateService) {
        var self = this;

        /**
         * List of projects.
         * @type {Array.<Object>}
         */
        self.projects = [];
        $scope.$watchCollection(
                function() {return ProjectsService.projects},
                function(newVal) {self.projects = newVal}
        );

        $scope.getIconColorStyle = function(color) {
            return {'fill':color};
        };

        /**
         * Indicates that the project data is being loaded.
         * @type {boolean}
         */
        self.loading = true;

        /**
         * Query for filtering projects.
         * @type {string}
         */
        self.searchQuery = "";

        /**
         * Navigates to the details page for the specified project.
         *
         * @param {Object} project the project
         */
        self.editProject = function(project) {
            StateService.FeedManager().Project().navigateToProjectDetails(project.id);
        };

        // Register Add button
        AccessControlService.getUserAllowedActions()
                .then(function(actionSet) {
                    // TODO: [x] re-enable PROJECTS_EDIT check, after figuring out how to give Admin group editProjects perms
                    if (AccessControlService.hasAction(AccessControlService.PROJECTS_EDIT, actionSet.actions)) {
                        AddButtonService.registerAddButton('projects', function () {
                            StateService.FeedManager().Project().navigateToProjectDetails(null);
                        });
                    }
                });

        // Refresh list of projects
        ProjectsService.reload()
                .then(function() {
                    self.loading = false;
                });
    };

    angular.module(moduleName).controller('ProjectsController', ["$scope","AccessControlService","AddButtonService","ProjectsService","StateService",ProjectsController]);
});

